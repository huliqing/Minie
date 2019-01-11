/*
 Copyright (c) 2019, Stephen Gold
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

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.animation.DynamicAnimControl;
import com.jme3.light.DirectionalLight;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

/**
 * A very simple example using DynamicAnimControl.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class HelloDac extends SimpleApplication {

    public static void main(String[] arguments) {
        HelloDac application = new HelloDac();
        application.start();
    }

    @Override
    public void simpleInitApp() {
        // Set up Bullet physics (with debug enabled).
        BulletAppState bulletAppState = new BulletAppState();
        bulletAppState.setDebugEnabled(true);
        bulletAppState.setEnabled(true);
        stateManager.attach(bulletAppState);
        PhysicsSpace physicsSpace = bulletAppState.getPhysicsSpace();

        // Add a light to the scene.
        Vector3f direction = new Vector3f(1f, -2f, -1f).normalizeLocal();
        DirectionalLight sun = new DirectionalLight(direction);
        rootNode.addLight(sun);

        // Add a model to the scene.
        Spatial ninjaModel
                = assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
        rootNode.attachChild(ninjaModel);
        ninjaModel.rotate(0f, 3f, 0f);
        ninjaModel.scale(0.02f);

        // Add a DynamicAnimControl to the model.
        DynamicAnimControl dac = new DynamicAnimControl();
        ninjaModel.addControl(dac);
        dac.setPhysicsSpace(physicsSpace);

        // Because no linked bones are specified, the model behaves more like a
        // rigid body than a ragdoll.
    }
}