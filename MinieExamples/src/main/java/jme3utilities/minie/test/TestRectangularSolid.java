/*
 Copyright (c) 2018-2019, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.minie.test;

import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.collision.shapes.MultiSphere;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.DebugShapeFactory;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyAsset;
import jme3utilities.MyString;
import jme3utilities.math.RectangularSolid;
import jme3utilities.math.noise.Generator;
import jme3utilities.mesh.PointMesh;
import jme3utilities.ui.ActionApplication;
import jme3utilities.ui.InputMode;

/**
 * Test collision-shape fitting to pseudo-random ellipsoids using the
 * RectangularSolid class.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestRectangularSolid extends ActionApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * color for visualizing sample points (white)
     */
    final private static ColorRGBA sampleColor = new ColorRGBA(1f, 1f, 1f, 1f);
    /**
     * size for visualizing sample points (in pixels)
     */
    final private static float samplePointSize = 2f;
    /**
     * number of sample points per trial
     */
    final private static int samplesPerTrial = 100;
    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(TestRectangularSolid.class.getName());
    /**
     * application name for its window's title bar
     */
    final private static String applicationName
            = TestRectangularSolid.class.getSimpleName();
    // *************************************************************************
    // fields

    /**
     * scene-graph node for displaying user-interface text
     */
    private BitmapText uiText;
    /**
     * Bullet app state
     */
    private static BulletAppState bulletAppState;
    /**
     * enhanced pseudo-random generator
     */
    final private static Generator generator = new Generator();
    /*
     * pseudo-random seed for the current/next trial
     */
    private long trialSeed = 1L;
    /**
     * material for visualizing sample points
     */
    private static Material samplePointMaterial;
    /**
     * scene-graph node for the current trial
     */
    private static Node trialNode = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        /*
         * Mute the chatty loggers found in some imported packages.
         */
        Misc.setLoggingLevels(Level.WARNING);
        Logger.getLogger(ALAudioRenderer.class.getName())
                .setLevel(Level.SEVERE);

        TestRectangularSolid application = new TestRectangularSolid();
        /*
         * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
        String title = applicationName + " " + MyString.join(arguments);
        settings.setTitle(title);
        application.setSettings(settings);

        application.start();
    }
    // *************************************************************************
    // ActionApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void actionInitializeApplication() {
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(10f);
        /*
         * Add a BitmapText in the upper-left corner of the display.
         */
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        uiText = new BitmapText(font);
        guiNode.attachChild(uiText);
        float displayHeight = cam.getHeight();
        uiText.move(0f, displayHeight, 0f);

        samplePointMaterial = MyAsset.createWireframeMaterial(assetManager,
                sampleColor, samplePointSize);

        CollisionShape.setDefaultMargin(0.0001f);
        bulletAppState = new BulletAppState();
        bulletAppState.setDebugEnabled(true);
        bulletAppState.setSpeed(0f);
        stateManager.attach(bulletAppState);

        CameraOrbitAppState orbitState
                = new CameraOrbitAppState(cam, "orbitLeft", "orbitRight");
        stateManager.attach(orbitState);

        trial(true);
    }

    /**
     * Add application-specific hotkey bindings and override existing ones.
     */
    @Override
    public void moreDefaultBindings() {
        InputMode dim = getDefaultInputMode();

        dim.bind("next trial rounded", KeyInput.KEY_F1);
        dim.bind("next trial square", KeyInput.KEY_F2);
        dim.bind("signal orbitLeft", KeyInput.KEY_LEFT);
        dim.bind("signal orbitRight", KeyInput.KEY_RIGHT);
    }

    /**
     * Process an action that wasn't handled by the active input mode.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing) {
            switch (actionString) {
                case "next trial rounded":
                    nextTrial(true);
                    return;
                case "next trial square":
                    nextTrial(false);
                    return;
            }
        }
        super.onAction(actionString, ongoing, tpf);
    }
    // *************************************************************************
    // private methods

    /**
     * Enumerate the corner locations of the specified RectangularSolid.
     *
     * @param rectangularSolid (not null)
     * @return a new collection of new vectors
     */
    private Collection<Vector3f> listCorners(RectangularSolid rectangularSolid) {
        Vector3f maxima = rectangularSolid.maxima(null);
        Vector3f minima = rectangularSolid.minima(null);
        /*
         * Enumerate the local coordinates of the 8 corners of the box.
         */
        Collection<Vector3f> cornerLocations = new ArrayList<>(8);
        cornerLocations.add(new Vector3f(maxima.x, maxima.y, maxima.z));
        cornerLocations.add(new Vector3f(maxima.x, maxima.y, minima.z));
        cornerLocations.add(new Vector3f(maxima.x, minima.y, maxima.z));
        cornerLocations.add(new Vector3f(maxima.x, minima.y, minima.z));
        cornerLocations.add(new Vector3f(minima.x, maxima.y, maxima.z));
        cornerLocations.add(new Vector3f(minima.x, maxima.y, minima.z));
        cornerLocations.add(new Vector3f(minima.x, minima.y, maxima.z));
        cornerLocations.add(new Vector3f(minima.x, minima.y, minima.z));
        /*
         * Transform corner locations to world coordinates.
         */
        Vector3f tempVector = new Vector3f();
        for (Vector3f location : cornerLocations) {
            rectangularSolid.localToWorld(location, tempVector);
            location.set(tempVector);
        }

        return cornerLocations;
    }

    /**
     * Perform a new trial after cleaning up from the previous one.
     *
     * @param roundCorners type of collision shape to generate: true &rarr;
     * MultiSphere with round corners, false &rarr; HullCollisionShape with
     * square corners
     */
    private void nextTrial(boolean roundCorners) {
        if (trialNode != null) {
            PhysicsSpace space = bulletAppState.getPhysicsSpace();
            space.removeAll(trialNode);
            trialNode.removeFromParent();
            trialNode = null;
        }
        trial(roundCorners);
    }

    /**
     * Perform a new trial.
     *
     * @param roundCorners type of collision shape to generate: true &rarr;
     * MultiSphere with round corners, false &rarr; HullCollisionShape with
     * square corners
     */
    private void trial(boolean roundCorners) {
        trialNode = new Node("trialNode");
        rootNode.attachChild(trialNode);

        String msg = String.format("trialSeed=%d", trialSeed);
        System.out.println(msg);
        uiText.setText(msg);
        generator.setSeed(trialSeed);
        /*
         * Generate a new transform.
         */
        Transform transform = new Transform();
        Quaternion rotation = generator.nextQuaternion();
        transform.setRotation(rotation);
        Vector3f scale = generator.nextVector3f();
        scale.addLocal(1.2f, 1.2f, 1.2f);
        scale.multLocal(2f);
        transform.setScale(scale);
        Vector3f translation = generator.nextVector3f();
        transform.setTranslation(translation);
        /*
         * Generate sample points on the surface of a transformed unit sphere
         * (which is an ellipsoid).
         */
        Collection<Vector3f> sampleLocations = new ArrayList<>(samplesPerTrial);
        for (int sampleIndex = 0; sampleIndex < samplesPerTrial; sampleIndex++) {
            Vector3f sampleLocation = generator.nextUnitVector3f();
            transform.transformVector(sampleLocation, sampleLocation);
            sampleLocations.add(sampleLocation);
        }
        /*
         * Visualize the sample points.
         */
        for (Vector3f location : sampleLocations) {
            PointMesh pointMesh = new PointMesh();
            pointMesh.setLocation(location);
            Geometry sampleGeometry = new Geometry("sample", pointMesh);
            sampleGeometry.setMaterial(samplePointMaterial);
            trialNode.attachChild(sampleGeometry);
        }
        /*
         * Generate a rectangular solid that contains all the samples.
         */
        RectangularSolid solid = new RectangularSolid(sampleLocations);
        logger.log(Level.INFO, solid.toString());
        /*
         * Generate a collision shape to match the rectangular solid.
         */
        CollisionShape collisionShape;
        if (roundCorners) {
            collisionShape = new MultiSphere(solid);
        } else {
            collisionShape = new HullCollisionShape(solid);
        }
        /*
         * Add a rigid-body control with that shape.
         */
        RigidBodyControl rbc = new RigidBodyControl(collisionShape);
        rbc.setDebugMeshResolution(DebugShapeFactory.highResolution);
        PhysicsSpace space = bulletAppState.getPhysicsSpace();
        rbc.setPhysicsSpace(space);
        trialNode.addControl(rbc);

        ++trialSeed;
    }
}
