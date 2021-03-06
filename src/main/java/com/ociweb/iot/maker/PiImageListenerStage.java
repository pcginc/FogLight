package com.ociweb.iot.maker;

import com.ociweb.iot.camera.Camera;
import com.ociweb.iot.camera.ProxyCam;
import com.ociweb.iot.camera.RaspiCam;
import com.ociweb.pronghorn.pipe.PipeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.pronghorn.iot.schema.ImageSchema;
import com.ociweb.pronghorn.pipe.Pipe;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Time-based image listener backing for Raspberry Pi hardware.
 *
 * This stage passes image frames line-by-line to its consumers.
 *
 * TODO: For total integration
 * - Build a new schema. --Done
 * - As user declares things, the system builds dangling pipes.
 *  - The other end of the pipe is not defined yet.
 *  - When you make the stage, the graph will register it.
 *  - If you run the graph, it'll complain because the stage has dangling pipes.
 *  - BuildGraph asks the graph for the dangling pipes by schema.
 *  - Those dangling pipes are then tied into something.
 *  - This design means we need a unique schema for each kind of special resource.
 * - ImageListener registration creates an instance of the reactor stage.
 *  - That stage needs attached to it an image listener pipe.
 *  - When the graph is built, we track all of the listeners and attach the pipe.
 *  - Use the replicator stage to take one pipe (from picture taker stage) and feed it to the listeners if there
 *    are more than one listener.
 *
 * @author Brandon Sanders [brandon@alicorn.io]
 */
public class PiImageListenerStage extends PronghornStage {

    private static final Logger logger = LoggerFactory.getLogger(PiImageListenerStage.class);

    // Output pipe for image data.
    private final Pipe<ImageSchema> output;

    // Camera system.
    private boolean v4l2Available = true;
    private Camera camera;
    private int cameraFd;

    // Image buffer information; we only process one image at a time.
    private byte[] frameBytes = null;
    private int frameBytesPublishHead = -1;

    public static final int FRAME_WIDTH = 1920;
    public static final int FRAME_HEIGHT = 1080;
    public static final int ROW_SIZE = FRAME_WIDTH * 3;

    public PiImageListenerStage(GraphManager graphManager, Pipe<ImageSchema> output, int triggerRateMilliseconds) {
        super(graphManager, NONE, output);

        // Attach to our output pipe.
        this.output = output;

        // Add this listener to the graph.
        GraphManager.addNota(graphManager, GraphManager.SCHEDULE_RATE, triggerRateMilliseconds * 1000000L, this);
    }

    @Override
    public void startup() {
        // TODO: Open camera here.
        frameBytes = new byte[1080 * 1920 * 3];

        // Get a file for the default camera device.
        File cameraFile = Paths.get(RaspiCam.DEFAULT_CAMERA_DEVICE).toFile();

        // Open /dev/video0 on Raspberry Pi.
        if (!cameraFile.exists()) {

            // Load V4L2 module.
            try {
                Runtime.getRuntime().exec("modprobe bcm2835-v4l2").waitFor();
            } catch (IOException | InterruptedException e) {
                logger.error(e.getMessage(), e);
            }

            // If it still isn't loaded, disable V4L2.
            if (!cameraFile.exists()) {
                v4l2Available = false;
            }
        }

        // Open camera interface.
        if (v4l2Available) {
            camera = new RaspiCam();
            cameraFd = camera.open(RaspiCam.DEFAULT_CAMERA_DEVICE, FRAME_WIDTH, FRAME_HEIGHT);
        } else {
            camera = new ProxyCam();
            cameraFd = camera.open("./images", FRAME_WIDTH, FRAME_HEIGHT);
        }

        // Configure byte array for camera frames.
        frameBytes = new byte[camera.getFrameSizeBytes(cameraFd)];
    }

    @Override
    public void shutdown() {
		if (Pipe.hasRoomForWrite(output, Pipe.EOF_SIZE)) {
			Pipe.publishEOF(output);
		}

		// Close camera.
        camera.close(cameraFd);
    }

    @Override
    public void run() {

        // Only execute while we have room to write our output.
        if (Pipe.hasRoomForWrite(output)) {

            // If there's no frame, read one and publish start.
            if (frameBytesPublishHead == -1) {

                // Attempt a frame capture.
                if (camera.readFrame(cameraFd, frameBytes, 0) != -1) {

                    // If a frame was available, reset head to the beginning.
                    frameBytesPublishHead = 0;

                    // Publish frame start.
                    if (PipeWriter.tryWriteFragment(output, ImageSchema.MSG_FRAMESTART_1)) {
                        PipeWriter.writeInt(output, ImageSchema.MSG_FRAMESTART_1_FIELD_WIDTH_101, FRAME_WIDTH);
                        PipeWriter.writeInt(output, ImageSchema.MSG_FRAMESTART_1_FIELD_HEIGHT_201, FRAME_HEIGHT);
                        PipeWriter.writeLong(output, ImageSchema.MSG_FRAMESTART_1_FIELD_TIMESTAMP_301, System.currentTimeMillis());
                        PipeWriter.publishWrites(output);
                    }
                }

            // Otherwise, write a frame part if there's space.
            } else if (PipeWriter.tryWriteFragment(output, ImageSchema.MSG_FRAMECHUNK_2) &&
                       PipeWriter.hasRoomForFragmentOfSize(output, ROW_SIZE)) {
                PipeWriter.writeBytes(output, ImageSchema.MSG_FRAMECHUNK_2_FIELD_ROWBYTES_102, frameBytes, frameBytesPublishHead, ROW_SIZE);
                PipeWriter.publishWrites(output);

                // Progress head.
                frameBytesPublishHead += ROW_SIZE;

                // If the head exceeds the size of the frame bytes, we're done writing.
                if (frameBytesPublishHead >= frameBytes.length) {
                    frameBytesPublishHead = -1;
                }
            }
        }
    }
}
