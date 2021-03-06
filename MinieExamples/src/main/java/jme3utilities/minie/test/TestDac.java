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

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.SkeletonControl;
import com.jme3.app.StatsAppState;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.ModelKey;
import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.animation.BoneLink;
import com.jme3.bullet.animation.CenterHeuristic;
import com.jme3.bullet.animation.DynamicAnimControl;
import com.jme3.bullet.animation.LinkConfig;
import com.jme3.bullet.animation.MassHeuristic;
import com.jme3.bullet.animation.ShapeHeuristic;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.joints.Point2PointJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.JmeExporter;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.xml.XMLExporter;
import com.jme3.input.KeyInput;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.ogre.MaterialLoader;
import com.jme3.scene.plugins.ogre.MeshLoader;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.system.AppSettings;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.NameGenerator;
import jme3utilities.debug.SkeletonVisualizer;
import jme3utilities.math.MyVector3f;
import jme3utilities.math.noise.Generator;
import jme3utilities.minie.PhysicsDumper;
import jme3utilities.minie.test.tunings.Biped;
import jme3utilities.minie.test.tunings.CesiumManControl;
import jme3utilities.minie.test.tunings.ElephantControl;
import jme3utilities.minie.test.tunings.JaimeControl;
import jme3utilities.minie.test.tunings.MhGameControl;
import jme3utilities.minie.test.tunings.NinjaControl;
import jme3utilities.minie.test.tunings.OtoControl;
import jme3utilities.minie.test.tunings.PuppetControl;
import jme3utilities.minie.test.tunings.SinbadControl;
import jme3utilities.ui.ActionApplication;
import jme3utilities.ui.InputMode;
import jme3utilities.ui.Signals;

/**
 * Test scaling and load/save on a DynamicAnimControl.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestDac extends ActionApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(TestDac.class.getName());
    /**
     * asset path for saving J3O
     */
    final private static String saveAssetPath = "TestDac.j3o";
    /**
     * 1st asset path for saving XML
     */
    final private static String saveAssetPath1 = "TestDac_1.xml";
    /**
     * 2nd asset path for saving XML
     */
    final private static String saveAssetPath2 = "TestDac_2.xml";
    /**
     * application name for its window's title bar
     */
    final private static String applicationName
            = TestDac.class.getSimpleName();
    // *************************************************************************
    // fields

    private AnimChannel animChannel = null;
    private BoneLink leftClavicle;
    private BoneLink leftFemur;
    private BoneLink leftUlna;
    private BoneLink rightClavicle;
    private BoneLink rightFemur;
    private BoneLink upperBody;
    private BulletAppState bulletAppState;
    private CollisionShape ballShape;
    /**
     * control being tested
     */
    private DynamicAnimControl dac;
    final private float ballRadius = 0.2f; // mesh units
    /**
     * random number generator for this application
     */
    final private Generator rng = new Generator();
    private Material ballMaterial;
    final private Mesh ballMesh = new Sphere(16, 32, ballRadius);
    final private NameGenerator nameGenerator = new NameGenerator();
    private Node cgModel;
    private PhysicsSpace physicsSpace;
    private SkeletonControl sc;
    private SkeletonVisualizer sv;
    private String animationName = null;
    /**
     * name some important linked bones
     */
    private String leftClavicleName;
    private String leftUlnaName;
    private String rightClavicleName;
    private String upperBodyName;
    private Transform resetTransform;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        /*
         * Mute the chatty loggers found in some imported packages.
         */
        Misc.setLoggingLevels(Level.WARNING);
        Logger.getLogger(MaterialLoader.class.getName()).setLevel(Level.SEVERE);
        Logger.getLogger(MeshLoader.class.getName()).setLevel(Level.SEVERE);
        Logger.getLogger(ALAudioRenderer.class.getName())
                .setLevel(Level.SEVERE);

        TestDac application = new TestDac();
        /*
         * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
        String title = applicationName + " " + MyString.join(arguments);
        settings.setTitle(title);

        settings.setVSync(true);
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
        configureCamera();
        configurePhysics();
        viewPort.setBackgroundColor(ColorRGBA.Gray);
        addLighting();

        stateManager.getState(StatsAppState.class).toggleStats();

        ColorRGBA ballColor = new ColorRGBA(0.4f, 0f, 0f, 1f);
        ballMaterial = MyAsset.createShinyMaterial(assetManager, ballColor);
        ballMaterial.setFloat("Shininess", 5f);
        ballShape = new SphereCollisionShape(ballRadius);

        addBox();
        addModel("Sinbad");
    }

    /**
     * Add application-specific hotkey bindings and override existing ones.
     */
    @Override
    public void moreDefaultBindings() {
        InputMode dim = getDefaultInputMode();

        dim.bind("amputate left elbow", KeyInput.KEY_DELETE);
        dim.bind("blend all to kinematic", KeyInput.KEY_K);
        dim.bind("drop attachments", KeyInput.KEY_PGDN);
        dim.bind("dump physicsSpace", KeyInput.KEY_O);
        dim.bind("dump scenes", KeyInput.KEY_P);
        dim.bind("freeze all", KeyInput.KEY_F);
        dim.bind("freeze upper body", KeyInput.KEY_U);
        dim.bind("ghost upper body", KeyInput.KEY_8);
        dim.bind("go bind pose", KeyInput.KEY_B);
        dim.bind("go floating", KeyInput.KEY_0);
        dim.bind("go limp", KeyInput.KEY_SPACE);
        dim.bind("limp left arm", KeyInput.KEY_LBRACKET);
        dim.bind("limp right arm", KeyInput.KEY_RBRACKET);
        dim.bind("load", KeyInput.KEY_L);
        dim.bind("load CesiumMan", KeyInput.KEY_F12);
        dim.bind("load Elephant", KeyInput.KEY_F3);
        dim.bind("load Jaime", KeyInput.KEY_F2);
        dim.bind("load MhGame", KeyInput.KEY_F9);
        dim.bind("load Ninja", KeyInput.KEY_F7);
        dim.bind("load Oto", KeyInput.KEY_F6);
        dim.bind("load Puppet", KeyInput.KEY_F8);
        dim.bind("load Sinbad", KeyInput.KEY_F1);
        dim.bind("load SinbadWith1Sword", KeyInput.KEY_F10);
        dim.bind("load SinbadWithSwords", KeyInput.KEY_F4);
        dim.bind("pin leftFemur", KeyInput.KEY_9);
        dim.bind("raise leftFoot", KeyInput.KEY_LCONTROL);
        dim.bind("raise leftHand", KeyInput.KEY_LSHIFT);
        dim.bind("raise rightFoot", KeyInput.KEY_RCONTROL);
        dim.bind("raise rightHand", KeyInput.KEY_RSHIFT);
        dim.bind("reset model transform", KeyInput.KEY_DOWN);
        dim.bind("save", KeyInput.KEY_SEMICOLON);
        dim.bind("set height 1", KeyInput.KEY_1);
        dim.bind("set height 2", KeyInput.KEY_2);
        dim.bind("set height 3", KeyInput.KEY_3);
        dim.bind("signal rotateLeft", KeyInput.KEY_LEFT);
        dim.bind("signal rotateRight", KeyInput.KEY_RIGHT);
        dim.bind("signal shower", KeyInput.KEY_INSERT);
        dim.bind("toggle meshes", KeyInput.KEY_M);
        dim.bind("toggle pause", KeyInput.KEY_PERIOD);
        dim.bind("toggle physics debug", KeyInput.KEY_SLASH);
        dim.bind("toggle skeleton", KeyInput.KEY_V);
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
                case "amputate left elbow":
                    dac.amputateSubtree(leftUlna, 2f);
                    return;
                case "blend all to kinematic":
                    dac.blendToKinematicMode(2f, null);
                    return;
                case "drop attachments":
                    dac.dropAttachments();
                    return;
                case "dump physicsSpace":
                    dumpPhysicsSpace();
                    return;
                case "dump scenes":
                    dumpScenes();
                    return;
                case "freeze all":
                    dac.freezeSubtree(dac.getTorsoLink(), false);
                    return;
                case "freeze upper body":
                    dac.freezeSubtree(upperBody, false);
                    return;
                case "ghost upper body":
                    dac.setContactResponseSubtree(upperBody, false);
                    return;
                case "go bind pose":
                    dac.bindSubtree(dac.getTorsoLink(), 2f);
                    return;

                case "go floating":
                    if (dac.isReady()) {
                        dac.setDynamicSubtree(dac.getTorsoLink(), Vector3f.ZERO,
                                false);
                    }
                    return;
                case "go limp":
                    if (dac.isReady()) {
                        dac.setRagdollMode();
                    }
                    return;
                case "limp left arm":
                    if (dac.isReady()) {
                        dac.setDynamicSubtree(leftClavicle,
                                new Vector3f(0f, -30f, 0f), false);
                    }
                    return;
                case "limp right arm":
                    if (dac.isReady()) {
                        dac.setDynamicSubtree(rightClavicle,
                                new Vector3f(0f, -30f, 0f), false);
                    }
                    return;

                case "load":
                    load();
                    return;
                case "pin leftFemur":
                    pin(leftFemur);
                    return;

                case "raise leftFoot":
                    if (dac.isReady()) {
                        dac.setDynamicSubtree(leftFemur,
                                new Vector3f(0f, 20f, 0f), false);
                    }
                    return;
                case "raise leftHand":
                    if (dac.isReady()) {
                        dac.setDynamicSubtree(leftClavicle,
                                new Vector3f(0f, 20f, 0f), false);
                    }
                    return;
                case "raise rightFoot":
                    if (dac.isReady()) {
                        dac.setDynamicSubtree(rightFemur,
                                new Vector3f(0f, 20f, 0f), false);
                    }
                    return;
                case "raise rightHand":
                    if (dac.isReady()) {
                        dac.setDynamicSubtree(rightClavicle,
                                new Vector3f(0f, 20f, 0f), false);
                    }
                    return;

                case "reset model transform":
                    cgModel.setLocalTransform(resetTransform);
                    return;
                case "save":
                    save(cgModel, saveAssetPath);
                    save(cgModel, saveAssetPath1);
                    return;
                case "set height 1":
                    setHeight(1f);
                    return;
                case "set height 2":
                    setHeight(2f);
                    return;
                case "set height 3":
                    setHeight(3f);
                    return;
                case "toggle meshes":
                    toggleMeshes();
                    return;
                case "toggle pause":
                    togglePause();
                    return;
                case "toggle physics debug":
                    togglePhysicsDebug();
                    return;
                case "toggle skeleton":
                    toggleSkeleton();
                    return;
            }
            String[] words = actionString.split(" ");
            if (words.length >= 2 && "load".equals(words[0])) {
                addModel(words[1]);
                return;
            }
        }
        super.onAction(actionString, ongoing, tpf);
    }

    /**
     * Callback invoked once per frame.
     *
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);

        Signals signals = getSignals();
        if (signals.test("shower")) {
            addBall();
        }

        float rotateAngle = 0f;
        if (signals.test("rotateRight")) {
            rotateAngle += tpf;
        }
        if (signals.test("rotateLeft")) {
            rotateAngle -= tpf;
        }
        if (rotateAngle != 0f) {
            rotateAngle /= speed;
            Quaternion orientation = MySpatial.worldOrientation(cgModel, null);
            Quaternion rotate = new Quaternion();
            rotate.fromAngles(0f, rotateAngle, 0f);
            rotate.mult(orientation, orientation);
            MySpatial.setWorldOrientation(cgModel, orientation);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Add a falling ball to the scene.
     */
    private void addBall() {
        String name = nameGenerator.unique("ball");
        Geometry geometry = new Geometry(name, ballMesh);
        rootNode.attachChild(geometry);

        geometry.setMaterial(ballMaterial);
        geometry.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        Vector3f location = rng.nextVector3f();
        location.multLocal(0.5f, 1f, 0.5f);
        location.y += 4f;
        geometry.move(location);

        Vector3f worldScale = geometry.getWorldScale();
        ballShape.setScale(worldScale);
        float mass = 0.1f;
        RigidBodyControl rbc = new RigidBodyControl(ballShape, mass);
        rbc.setApplyScale(true);
        rbc.setLinearVelocity(new Vector3f(0f, -1f, 0f));
        rbc.setKinematic(false);

        rbc.setPhysicsSpace(physicsSpace);
        rbc.setGravity(new Vector3f(0f, -1f, 0f));

        geometry.addControl(rbc);
    }

    /**
     * Add a large static box to serve as a platform.
     */
    private void addBox() {
        float halfExtent = 50f; // mesh units
        Mesh mesh = new Box(halfExtent, halfExtent, halfExtent);
        Geometry geometry = new Geometry("box", mesh);
        rootNode.attachChild(geometry);

        geometry.move(0f, -halfExtent, 0f);
        ColorRGBA color = new ColorRGBA(0.1f, 0.4f, 0.1f, 1f);
        Material material = MyAsset.createShadedMaterial(assetManager, color);
        geometry.setMaterial(material);
        geometry.setShadowMode(RenderQueue.ShadowMode.Receive);

        Vector3f hes = new Vector3f(halfExtent, halfExtent, halfExtent);
        BoxCollisionShape shape = new BoxCollisionShape(hes);
        float mass = PhysicsRigidBody.massForStatic;
        RigidBodyControl rbc = new RigidBodyControl(shape, mass);
        rbc.setApplyScale(true);
        rbc.setPhysicsSpace(physicsSpace);
        geometry.addControl(rbc);
    }

    /**
     * Add lighting and shadows to the scene.
     */
    private void addLighting() {
        ColorRGBA ambientColor = new ColorRGBA(0.7f, 0.7f, 0.7f, 1f);
        AmbientLight ambient = new AmbientLight(ambientColor);
        rootNode.addLight(ambient);

        Vector3f direction = new Vector3f(1f, -2f, -1f).normalizeLocal();
        DirectionalLight sun = new DirectionalLight(direction);
        rootNode.addLight(sun);

        DirectionalLightShadowRenderer dlsr
                = new DirectionalLightShadowRenderer(assetManager, 4_096, 3);
        dlsr.setLight(sun);
        dlsr.setShadowIntensity(0.5f);
        viewPort.addProcessor(dlsr);
    }

    /**
     * Add an animated model to the scene, removing any previously added model.
     *
     * @param modelName the name of the model to add (not null, not empty)
     */
    private void addModel(String modelName) {
        if (cgModel != null) {
            dac.getSpatial().removeControl(dac);
            rootNode.detachChild(cgModel);
            rootNode.removeControl(sv);
            removeAllBalls();
        }

        switch (modelName) {
            case "CesiumMan":
                loadCesiumMan();
                break;
            case "Elephant":
                loadElephant();
                break;
            case "Jaime":
                loadJaime();
                break;
            case "MhGame":
                loadMhGame();
                break;
            case "Ninja":
                loadNinja();
                break;
            case "Oto":
                loadOto();
                break;
            case "Puppet":
                loadPuppet();
                break;
            case "Sinbad":
                loadSinbad();
                break;
            case "SinbadWith1Sword":
                loadSinbadWith1Sword();
                break;
            case "SinbadWithSwords":
                loadSinbadWithSwords();
                break;
            default:
                throw new IllegalArgumentException(modelName);
        }

        List<Spatial> list
                = MySpatial.listSpatials(cgModel, Spatial.class, null);
        for (Spatial spatial : list) {
            spatial.setShadowMode(RenderQueue.ShadowMode.Cast);
        }
        cgModel.setCullHint(Spatial.CullHint.Never);

        rootNode.attachChild(cgModel);
        setHeight(cgModel, 2f);
        center(cgModel);
        resetTransform = cgModel.getLocalTransform().clone();

        List<SkeletonControl> scList
                = MySpatial.listControls(cgModel, SkeletonControl.class, null);
        assert scList.size() == 1;
        sc = scList.get(0);
        Spatial controlledSpatial = sc.getSpatial();

        controlledSpatial.addControl(dac);
        dac.setPhysicsSpace(physicsSpace);

        leftClavicle = dac.findBoneLink(leftClavicleName);
        if (dac instanceof Biped) {
            BoneLink leftFoot = ((Biped) dac).getLeftFoot();
            leftFemur = leftFoot;
            while (leftFemur.getParent() instanceof BoneLink) {
                leftFemur = (BoneLink) leftFemur.getParent();
            }

            BoneLink rightFoot = ((Biped) dac).getRightFoot();
            rightFemur = rightFoot;
            while (rightFemur.getParent() instanceof BoneLink) {
                rightFemur = (BoneLink) rightFemur.getParent();
            }

        } else if (dac instanceof ElephantControl) {
            leftFemur = dac.findBoneLink("Oberschenkel_B_L");
            rightFemur = dac.findBoneLink("Oberschenkel_B_R");
        }

        leftUlna = dac.findBoneLink(leftUlnaName);
        rightClavicle = dac.findBoneLink(rightClavicleName);
        upperBody = dac.findBoneLink(upperBodyName);

        AnimControl animControl = controlledSpatial.getControl(AnimControl.class);
        animChannel = animControl.createChannel();
        animChannel.setAnim(animationName);

        sv = new SkeletonVisualizer(assetManager, sc);
        sv.setLineColor(ColorRGBA.Yellow); // TODO clean up visualization
        rootNode.addControl(sv);
    }

    /**
     * Translate a model's center so that the model rests on the X-Z plane, and
     * its center lies on the Y axis.
     */
    private void center(Spatial model) {
        Vector3f[] minMax = MySpatial.findMinMaxCoords(model);
        Vector3f center = MyVector3f.midpoint(minMax[0], minMax[1]);
        Vector3f offset = new Vector3f(center.x, minMax[0].y, center.z);

        Vector3f location = model.getWorldTranslation();
        location.subtractLocal(offset);
        MySpatial.setWorldLocation(model, location);
    }

    /**
     * Configure the camera during startup.
     */
    private void configureCamera() {
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(4f);

        cam.setLocation(new Vector3f(0f, 1.2f, 5f));
    }

    /**
     * Configure physics during startup.
     */
    private void configurePhysics() {
        CollisionShape.setDefaultMargin(0.005f); // 5 mm margin

        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        physicsSpace = bulletAppState.getPhysicsSpace();
        physicsSpace.setSolverNumIterations(15);
        physicsSpace.setAccuracy(0.01f); // 10 msec timestep
    }

    /**
     * Process a "dump physicsSpace" action.
     */
    private void dumpPhysicsSpace() {
        PhysicsDumper dumper = new PhysicsDumper();
        dumper.dump(physicsSpace);
    }

    /**
     * Process a "dump scenes" action.
     */
    private void dumpScenes() {
        PhysicsDumper dumper = new PhysicsDumper();
        //dumper.setDumpBucket(true);
        //dumper.setDumpCull(true);
        //dumper.setDumpMatParam(true);
        //dumper.setDumpOverride(true);
        //dumper.setDumpShadow(true);
        dumper.setDumpTransform(true);
        //dumper.setDumpUser(true);
        dumper.dump(renderManager);
    }

    /**
     * Load the saved model from the J3O file.
     */
    private void load() {
        ModelKey key = new ModelKey(saveAssetPath);
        /*
         * Remove any copy from the asset manager's cache.
         */
        assetManager.deleteFromCache(key);

        Spatial loadedScene;
        try {
            loadedScene = assetManager.loadAsset(key);
        } catch (AssetNotFoundException e) {
            logger.log(Level.SEVERE, "Didn''t find asset {0}",
                    MyString.quote(saveAssetPath));
            return;
        }
        logger.log(Level.INFO, "Loaded {0} from asset {1}", new Object[]{
            MyString.quote(loadedScene.getName()),
            MyString.quote(saveAssetPath)
        });

        save(loadedScene, saveAssetPath2);
    }

    /**
     * Load the CesiumMan model.
     */
    private void loadCesiumMan() {
        cgModel = (Node) assetManager.loadModel(
                "Models/CesiumMan/glTF-Binary/CesiumMan.glb");
        cgModel.rotate(0f, -1.6f, 0f);
        dac = new CesiumManControl();
        animationName = "anim_0";
        leftClavicleName = "Bone_16";
        leftUlnaName = "Bone_17";
        rightClavicleName = "Bone_13";
        upperBodyName = "Bone_11";
    }

    /**
     * Load the Elephant model.
     */
    private void loadElephant() {
        cgModel = (Node) assetManager.loadModel(
                "Models/Elephant/Elephant.mesh.xml");
        cgModel.rotate(0f, 1.6f, 0f);
        dac = new ElephantControl();
        animationName = "legUp";
        leftClavicleName = "Oberschenkel_F_L";
        leftUlnaName = "Knee_F_L";
        rightClavicleName = "Oberschenkel_F_R";
        upperBodyName = "joint5";
    }

    /**
     * Load the Jaime model.
     */
    private void loadJaime() {
        cgModel = (Node) assetManager.loadModel("Models/Jaime/Jaime.j3o");
        Geometry g = (Geometry) cgModel.getChild(0);
        RenderState rs = g.getMaterial().getAdditionalRenderState();
        rs.setFaceCullMode(RenderState.FaceCullMode.Off);

        dac = new JaimeControl();
        animationName = "Punches";
        leftClavicleName = "shoulder.L";
        leftUlnaName = "forearm.L";
        rightClavicleName = "shoulder.R";
        upperBodyName = "ribs";
    }

    /**
     * Load the MhGame model.
     */
    private void loadMhGame() {
        cgModel = (Node) assetManager.loadModel(
                "Models/MhGame/MhGame.mesh.xml");
        dac = new MhGameControl();
        animationName = "expr-lib-pose";
        leftClavicleName = "upperarm_l";
        leftUlnaName = "lowerarm_l";
        rightClavicleName = "upperarm_r";
        upperBodyName = "spine_01";
    }

    /**
     * Load the Ninja model.
     */
    private void loadNinja() {
        cgModel = (Node) assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
        cgModel.rotate(0f, 3f, 0f);
        dac = new NinjaControl();
        animationName = "Walk";
        leftClavicleName = "Joint14";
        leftUlnaName = "Joint16";
        rightClavicleName = "Joint9";
        upperBodyName = "Joint4";
    }

    /**
     * Load the Oto model.
     */
    private void loadOto() {
        cgModel = (Node) assetManager.loadModel("Models/Oto/Oto.mesh.xml");
        dac = new OtoControl();
        animationName = "Walk";
        leftClavicleName = "uparm.left";
        leftUlnaName = "arm.left";
        rightClavicleName = "uparm.right";
        upperBodyName = "spinehigh";
    }

    /**
     * Load the Puppet model.
     */
    private void loadPuppet() {
        cgModel = (Node) assetManager.loadModel("Models/Puppet/Puppet.j3o");
        dac = new PuppetControl();
        animationName = "walk";
        leftClavicleName = "upper_arm.1.L";
        leftUlnaName = "forearm.1.L";
        rightClavicleName = "upper_arm.1.R";
        upperBodyName = "spine";
    }

    /**
     * Load the Sinbad model without attachments.
     */
    private void loadSinbad() {
        cgModel = (Node) assetManager.loadModel(
                "Models/Sinbad/Sinbad.mesh.xml");
        dac = new SinbadControl();
        animationName = "Dance";
        leftClavicleName = "Clavicle.L";
        leftUlnaName = "Ulna.L";
        rightClavicleName = "Clavicle.R";
        upperBodyName = "Waist";
    }

    /**
     * Load the Sinbad model with an attached sword.
     */
    private void loadSinbadWith1Sword() {
        cgModel = (Node) assetManager.loadModel(
                "Models/Sinbad/Sinbad.mesh.xml");

        Node sword = (Node) assetManager.loadModel(
                "Models/Sinbad/Sword.mesh.xml");
        List<Spatial> list
                = MySpatial.listSpatials(sword, Spatial.class, null);
        for (Spatial spatial : list) {
            spatial.setShadowMode(RenderQueue.ShadowMode.Cast);
        }

        LinkConfig swordConfig = new LinkConfig(5f, MassHeuristic.Density,
                ShapeHeuristic.VertexHull, Vector3f.UNIT_XYZ,
                CenterHeuristic.AABB);
        dac = new SinbadControl();
        dac.attach("Handle.R", swordConfig, sword);

        animationName = "IdleTop";
        leftClavicleName = "Clavicle.L";
        leftUlnaName = "Ulna.L";
        rightClavicleName = "Clavicle.R";
        upperBodyName = "Waist";
    }

    /**
     * Load the Sinbad model with 2 attached swords.
     */
    private void loadSinbadWithSwords() {
        cgModel = (Node) assetManager.loadModel(
                "Models/Sinbad/Sinbad.mesh.xml");

        Node sword = (Node) assetManager.loadModel(
                "Models/Sinbad/Sword.mesh.xml");
        List<Spatial> list
                = MySpatial.listSpatials(sword, Spatial.class, null);
        for (Spatial spatial : list) {
            spatial.setShadowMode(RenderQueue.ShadowMode.Cast);
        }

        LinkConfig swordConfig = new LinkConfig(5f, MassHeuristic.Density,
                ShapeHeuristic.VertexHull, Vector3f.UNIT_XYZ,
                CenterHeuristic.AABB);
        dac = new SinbadControl();
        dac.attach("Handle.L", swordConfig, sword);
        dac.attach("Handle.R", swordConfig, sword);

        animationName = "RunTop";
        leftClavicleName = "Clavicle.L";
        leftUlnaName = "Ulna.L";
        rightClavicleName = "Clavicle.R";
        upperBodyName = "Waist";
    }

    /**
     * Pin the specified BoneLink to its current world location.
     *
     * @param boneLink (not null)
     */
    private void pin(BoneLink boneLink) {
        PhysicsRigidBody body = boneLink.getRigidBody();
        Vector3f currentWorld = body.getPhysicsLocation(null);
        Point2PointJoint sep2p
                = new Point2PointJoint(body, Vector3f.ZERO, currentWorld);
        body.addJoint(sep2p);
        physicsSpace.add(sep2p);
    }

    /**
     * Remove all balls from the scene.
     */
    private void removeAllBalls() {
        List<Geometry> geometries = rootNode.descendantMatches(Geometry.class);
        for (Geometry geometry : geometries) {
            String name = geometry.getName();
            if (NameGenerator.isFrom(name, "ball")) {
                RigidBodyControl rbc
                        = geometry.getControl(RigidBodyControl.class);
                rbc.setPhysicsSpace(null);
                geometry.removeControl(rbc);
                geometry.removeFromParent();
            }
        }
    }

    /**
     * Save the specified model to a J3O file.
     */
    private void save(Spatial model, String assetPath) {
        String filePath = ActionApplication.filePath(assetPath);
        File file = new File(filePath);

        JmeExporter exporter;
        if (assetPath.endsWith(".j3o")) {
            exporter = BinaryExporter.getInstance();
        } else {
            assert assetPath.endsWith(".xml");
            exporter = XMLExporter.getInstance();
        }

        try {
            exporter.save(model, file);
        } catch (IOException exception) {
            System.out.println("Caught IOException: " + exception.getMessage());
            logger.log(Level.SEVERE,
                    "Output exception while saving {0} to file {1}",
                    new Object[]{
                        MyString.quote(model.getName()),
                        MyString.quote(filePath)
                    });
            return;
        }
        logger.log(Level.INFO, "Saved {0} to file {1}", new Object[]{
            MyString.quote(model.getName()),
            MyString.quote(filePath)
        });
    }

    /**
     * Test re-scaling the model.
     */
    private void setHeight(float height) {
        assert height > 0f : height;

        setHeight(cgModel, height);
        center(cgModel);
        dac.rebuild();
    }

    /**
     * Scale the specified model uniformly so that it has the specified height.
     *
     * @param model (not null, modified)
     * @param height (in world units)
     */
    private void setHeight(Spatial model, float height) {
        Vector3f[] minMax = MySpatial.findMinMaxCoords(model);
        float oldHeight = minMax[1].y - minMax[0].y;

        model.scale(height / oldHeight);
    }

    /**
     * Toggle mesh rendering on/off.
     */
    private void toggleMeshes() {
        Spatial.CullHint hint = cgModel.getLocalCullHint();
        if (hint == Spatial.CullHint.Inherit
                || hint == Spatial.CullHint.Never) {
            hint = Spatial.CullHint.Always;
        } else if (hint == Spatial.CullHint.Always) {
            hint = Spatial.CullHint.Never;
        }
        cgModel.setCullHint(hint);
    }

    /**
     * Toggle the animation and physics simulation: paused/running.
     */
    private void togglePause() {
        float newSpeed = (speed > 1e-12f) ? 1e-12f : 1f;
        setSpeed(newSpeed);
    }

    /**
     * Toggle physics-debug visualization on/off.
     */
    private void togglePhysicsDebug() {
        boolean enabled = bulletAppState.isDebugEnabled();
        bulletAppState.setDebugEnabled(!enabled);
    }

    /**
     * Toggle the skeleton visualizer on/off.
     */
    private void toggleSkeleton() {
        boolean enabled = sv.isEnabled();
        sv.setEnabled(!enabled);
    }
}
