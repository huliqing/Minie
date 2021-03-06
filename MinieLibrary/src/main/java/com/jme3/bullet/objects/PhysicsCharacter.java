/*
 * Copyright (c) 2009-2019 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.bullet.objects;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.CollisionFlag;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;

/**
 * A collision object for simplified character simulation, based on Bullet's
 * btKinematicCharacterController.
 *
 * @author normenhansen
 */
public class PhysicsCharacter extends PhysicsCollisionObject {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger2
            = Logger.getLogger(PhysicsCharacter.class.getName());
    // *************************************************************************
    // fields

    /**
     * which convexSweepTest to use
     */
    private boolean isUsingGhostSweepTest = true;
    /**
     * Unique identifier of the btKinematicCharacterController (as opposed to
     * the collision object, which is a btPairCachingGhostObject). Constructors
     * are responsible for setting this to a non-zero value. The ID might change
     * if the character gets rebuilt.
     */
    private long characterId = 0L;
    /**
     * copy of the maximum amount of normal vertical movement (in physics-space
     * units)
     */
    private float stepHeight;
    private Vector3f walkOffset = new Vector3f();
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public PhysicsCharacter() {
    }

    /**
     * Instantiate a responsive character with the specified CollisionShape and
     * step height.
     *
     * @param shape the desired shape (not null, alias created)
     * @param stepHeight the maximum amount of normal vertical movement (in
     * physics-space units)
     */
    public PhysicsCharacter(CollisionShape shape, float stepHeight) {
        collisionShape = shape;
        this.stepHeight = stepHeight;
        buildObject();

        assert isContactResponse();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read this character's angular damping.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getAngularDamping() {
        return getAngularDamping(characterId);
    }

    /**
     * Copy this character's angular velocity.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the velocity vector (either storeResult or a new vector, not
     * null)
     */
    public Vector3f getAngularVelocity(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;
        getAngularVelocity(characterId, result);
        return result;
    }

    /**
     * Read the ID of the btKinematicCharacterController. Used internally.
     *
     * @return the unique identifier (not zero)
     */
    public long getControllerId() {
        return characterId;
    }

    /**
     * Read this character's fall speed.
     *
     * @return the speed (in physics-space units per second)
     */
    public float getFallSpeed() {
        return getFallSpeed(characterId);
    }

    /**
     * Copy this character's gravitational acceleration.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return an acceleration vector (in physics-space units per second
     * squared, in the direction opposite the "up" vector, either storeResult or
     * a new vector, not null)
     */
    public Vector3f getGravity(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;
        getGravity(characterId, result);
        return result;
    }

    /**
     * Read this character's jump speed.
     *
     * @return the speed (in physics-space units per second)
     */
    public float getJumpSpeed() {
        return getJumpSpeed(characterId);
    }

    /**
     * Read this character's linear damping.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getLinearDamping() {
        return getLinearDamping(characterId);
    }

    /**
     * Copy the linear velocity of this character's center.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return a vector (either storeResult or a new vector, not null)
     */
    public Vector3f getLinearVelocity(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;
        getLinearVelocity(characterId, result);
        return result;
    }

    /**
     * Read this character's maximum penetration depth.
     *
     * @return the depth (in physics-space units)
     */
    public float getMaxPenetrationDepth() {
        return getMaxPenetrationDepth(characterId);
    }

    /**
     * Read this character's maximum slope angle.
     *
     * @return the angle relative to the horizontal (in radians)
     */
    public float getMaxSlope() {
        return getMaxSlope(characterId);
    }

    /**
     * For compatability with the jme3-bullet library.
     *
     * @return a new location vector (in physics-space coordinates, not null)
     */
    public Vector3f getPhysicsLocation() {
        return getPhysicsLocation(null);
    }

    /**
     * Copy the location of this character's center.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return a location vector (in physics-space coordinates, either
     * storeResult or a new vector, not null)
     */
    public Vector3f getPhysicsLocation(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;
        getPhysicsLocation(objectId, result);

        assert Vector3f.isValidVector(result);
        return result;
    }

    /**
     * Read this character's step height.
     *
     * @return the maximum amount of normal vertical movement (in physics-space
     * units)
     */
    public float getStepHeight() {
        return stepHeight;
    }

    /**
     * Copy this character's "up" direction.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return a unit vector (in physics-space coordinates, in the direction
     * opposite the gravity vector, either storeResult or a new vector, not
     * null)
     */
    public Vector3f getUpDirection(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;
        getUpDirection(characterId, result);
        return result;
    }

    /**
     * Copy the character's walk offset.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return an offset vector (either storeResult or a new vector, not null)
     */
    public Vector3f getWalkDirection(Vector3f storeResult) {
        if (storeResult == null) {
            return walkOffset.clone();
        } else {
            return storeResult.set(walkOffset);
        }
    }

    /**
     * Test whether the ghost's convexSweepTest is in use.
     *
     * @return true if using the ghost's test, otherwise false
     */
    public boolean isUsingGhostSweepTest() {
        return isUsingGhostSweepTest;
    }

    /**
     * Jump in the specified direction.
     *
     * @param dir desired jump direction (not null, unaffected) or (0,0,0) to
     * use the "up" direction
     */
    public void jump(Vector3f dir) {
        jump(characterId, dir);
    }

    /**
     * Test whether this character is on the ground.
     *
     * @return true if on the ground, otherwise false
     */
    public boolean onGround() {
        return onGround(characterId);
    }

    /**
     * Reset this character, including its velocity.
     *
     * @param space (not null)
     */
    public void reset(PhysicsSpace space) {
        long spaceId = space.getSpaceId();
        reset(characterId, spaceId);
    }

    /**
     * Alter this character's angular damping.
     *
     * @param damping the desired viscous damping ratio (0&rarr;no damping,
     * 1&rarr;critically damped, default=0)
     */
    public void setAngularDamping(float damping) {
        setAngularDamping(characterId, damping);
    }

    /**
     * Alter this character's angular velocity.
     *
     * @param angularVelocity the desired angular velocity vector (not null,
     * unaffected)
     */
    public void setAngularVelocity(Vector3f angularVelocity) {
        setAngularVelocity(characterId, angularVelocity);
    }

    /**
     * Apply the specified CollisionShape to this character. Note that the
     * character should not be in any PhysicsSpace while changing shape; the
     * character gets rebuilt on the physics side.
     *
     * @param collisionShape the shape to apply (not null, alias created)
     */
    @Override
    public void setCollisionShape(CollisionShape collisionShape) {
        super.setCollisionShape(collisionShape);
        if (objectId == 0L) {
            buildObject();
        } else {
            attachCollisionShape(objectId, collisionShape.getObjectId());
        }
    }

    /**
     * Enable/disable this character's contact response.
     *
     * @param newState true to respond to contacts, false to ignore it
     * (default=true)
     */
    public void setContactResponse(boolean newState) {
        int flags = getCollisionFlags(objectId);
        if (newState) {
            flags &= ~CollisionFlag.NO_CONTACT_RESPONSE;
        } else {
            flags |= CollisionFlag.NO_CONTACT_RESPONSE;
        }
        setCollisionFlags(objectId, flags);
    }

    /**
     * Alter this character's fall speed.
     *
     * @param fallSpeed the desired speed (in physics-space units per second,
     * default=55)
     */
    public void setFallSpeed(float fallSpeed) {
        setFallSpeed(characterId, fallSpeed);
    }

    /**
     * Alter this character's gravitational acceleration. This may also alter
     * its "up" vector.
     *
     * @param gravity the desired acceleration vector (in physics-space units
     * per second squared, not null, unaffected, default=(0,0,-29.4))
     */
    public void setGravity(Vector3f gravity) {
        setGravity(characterId, gravity);
    }

    /**
     * Alter this character's jump speed.
     *
     * @param jumpSpeed the desired speed (in physics-space units per second,
     * default=10)
     */
    public void setJumpSpeed(float jumpSpeed) {
        setJumpSpeed(characterId, jumpSpeed);
    }

    /**
     * Alter this character's linear damping.
     *
     * @param damping the desired viscous damping ratio (0&rarr;no damping,
     * 1&rarr;critically damped, default=0)
     */
    public void setLinearDamping(float damping) {
        setLinearDamping(characterId, damping);
    }

    /**
     * Alter the linear velocity of this character's center.
     *
     * @param velocity the desired velocity vector (not null)
     */
    public void setLinearVelocity(Vector3f velocity) {
        setLinearVelocity(characterId, velocity);
    }

    /**
     * Alter this character's maximum penetration depth.
     *
     * @param depth the desired depth (in physics-space units, default=0.2)
     */
    public void setMaxPenetrationDepth(float depth) {
        setMaxPenetrationDepth(characterId, depth);
    }

    /**
     * Alter this character's maximum slope angle.
     *
     * @param slopeRadians the desired angle relative to the horizontal (in
     * radians, default=Pi/4)
     */
    public void setMaxSlope(float slopeRadians) {
        setMaxSlope(characterId, slopeRadians);
    }

    /**
     * Directly alter this character's location. (Same as
     * {@link #warp(com.jme3.math.Vector3f)}).)
     *
     * @param location the desired location (not null, unaffected)
     */
    public void setPhysicsLocation(Vector3f location) {
        warp(location);
    }

    /**
     * Alter this character's step height.
     *
     * @param height the desired maximum amount of normal vertical movement (in
     * physics-space units, default=1)
     */
    public void setStepHeight(float height) {
        this.stepHeight = height;
        setStepHeight(characterId, height);
    }

    /**
     * Alter which convexSweepTest is used.
     *
     * @param useGhostSweepTest true to use the ghost's test, false to use the
     * world's test (default=true)
     */
    public void setSweepTest(boolean useGhostSweepTest) {
        this.isUsingGhostSweepTest = useGhostSweepTest;
        setUseGhostSweepTest(characterId, useGhostSweepTest);
    }

    /**
     * Alter this character's "up" direction. This may also alter its gravity
     * vector.
     *
     * @param direction the desired direction (not null, not zero, unaffected,
     * default=(0,0,1))
     */
    public void setUp(Vector3f direction) {
        Validate.nonZero(direction, "direction");
        setUp(characterId, direction);
    }

    /**
     * Alter the walk offset. The offset will continue to be applied until
     * altered again.
     *
     * @param offset the desired position increment for each physics tick (not
     * null, unaffected)
     */
    public void setWalkDirection(Vector3f offset) {
        walkOffset.set(offset);
        setWalkDirection(characterId, offset);
    }

    /**
     * Directly alter the location of this character's center.
     *
     * @param location the desired physics location (not null, unaffected)
     */
    public void warp(Vector3f location) {
        warp(characterId, location);
    }
    // *************************************************************************
    // PhysicsCollisionObject methods

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned character into a deep-cloned one, using the specified
     * cloner and original to resolve copied fields.
     *
     * @param cloner the cloner that's cloning this body (not null)
     * @param original the instance from which this instance was shallow-cloned
     * (not null, unaffected)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);
        characterId = 0L;
        buildObject();
        walkOffset = cloner.clone(walkOffset);

        PhysicsCharacter old = (PhysicsCharacter) original;
        setAngularDamping(old.getAngularDamping());
        setAngularVelocity(old.getAngularVelocity(null));
        setCcdMotionThreshold(old.getCcdMotionThreshold());
        setCcdSweptSphereRadius(old.getCcdSweptSphereRadius());
        setContactResponse(old.isContactResponse());
        setFallSpeed(old.getFallSpeed());
        setGravity(old.getGravity(null));
        setJumpSpeed(old.getJumpSpeed());
        setLinearDamping(old.getLinearDamping());
        /*
         * Walk direction affects linear velocity, so set it first!
         */
        setWalkDirection(old.getWalkDirection(null));
        setLinearVelocity(old.getLinearVelocity(null));

        setMaxPenetrationDepth(old.getMaxPenetrationDepth());
        setMaxSlope(old.getMaxSlope());
        setPhysicsLocation(old.getPhysicsLocation(null));
        setStepHeight(old.getStepHeight());
        setSweepTest(old.isUsingGhostSweepTest());
        setUp(old.getUpDirection(null));
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public PhysicsCharacter jmeClone() {
        try {
            PhysicsCharacter clone = (PhysicsCharacter) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * De-serialize this character from the specified importer, for example when
     * loading from a J3O file.
     *
     * @param im the importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);

        InputCapsule capsule = im.getCapsule(this);
        stepHeight = capsule.readFloat("stepHeight", 1f);
        buildObject();

        setAngularDamping(capsule.readFloat("angularDamping", 0f));
        setAngularVelocity((Vector3f) capsule.readSavable("angularVelocity",
                new Vector3f()));
        setCcdMotionThreshold(capsule.readFloat("ccdMotionThreshold", 0f));
        setCcdSweptSphereRadius(capsule.readFloat("ccdSweptSphereRadius", 0f));
        setContactResponse(capsule.readBoolean("contactResponse", true));
        setFallSpeed(capsule.readFloat("fallSpeed", 55f));
        setSweepTest(capsule.readBoolean("ghostSweepTest", true));
        Vector3f g = (Vector3f) capsule.readSavable("gravityVector",
                new Vector3f(0f, -9.81f, 0f));
        setGravity(g);
        setJumpSpeed(capsule.readFloat("jumpSpeed", 10f));
        setLinearDamping(capsule.readFloat("linearDamping", 0f));
        /*
         * Walk direction affects linear velocity, so set it first!
         */
        setWalkDirection((Vector3f) capsule.readSavable("walkDirection",
                new Vector3f()));
        setLinearVelocity((Vector3f) capsule.readSavable("linearVelocity",
                new Vector3f()));

        setMaxPenetrationDepth(capsule.readFloat("maxPenetrationDepth", 0.2f));
        setMaxSlope(capsule.readFloat("maxSlope", FastMath.QUARTER_PI));
        setPhysicsLocation((Vector3f) capsule.readSavable("physicsLocation",
                new Vector3f()));
        if (MyVector3f.isZero(g)) {
            setUp((Vector3f) capsule.readSavable("upDirection",
                    new Vector3f(0f, 1f, 0f)));
        }
    }

    /**
     * Serialize this character, for example when saving to a J3O file.
     *
     * @param ex the exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);

        capsule.write(stepHeight, "stepHeight", 1f);

        capsule.write(getAngularDamping(), "angularDamping", 0f);
        capsule.write(getAngularVelocity(null), "angularVelocity", null);
        capsule.write(getCcdMotionThreshold(), "ccdMotionThreshold", 0f);
        capsule.write(getCcdSweptSphereRadius(), "ccdSweptSphereRadius", 0f);
        capsule.write(isContactResponse(), "contactResponse", true);
        capsule.write(getFallSpeed(), "fallSpeed", 55f);
        capsule.write(isUsingGhostSweepTest(), "ghostSweepTest", true);
        Vector3f g = getGravity(null);
        capsule.write(g, "gravityVector", new Vector3f(0f, -9.81f, 0f));
        capsule.write(getJumpSpeed(), "jumpSpeed", 10f);
        capsule.write(getLinearDamping(), "linearDamping", 0f);

        capsule.write(getWalkDirection(null), "walkDirection", null);
        capsule.write(getLinearVelocity(null), "linearVelocity", null);

        capsule.write(getMaxPenetrationDepth(), "maxPenetrationDepth", 0.2f);
        capsule.write(getMaxSlope(), "maxSlope", FastMath.QUARTER_PI);
        capsule.write(getPhysicsLocation(new Vector3f()), "physicsLocation",
                null);
        if (MyVector3f.isZero(g)) {
            capsule.write(getUpDirection(null), "upDirection",
                    new Vector3f(0f, 1f, 0f));
        }
    }
    // *************************************************************************
    // Object methods

    /**
     * Finalize this physics character just before it is destroyed. Should be
     * invoked only by a subclass or by the garbage collector.
     *
     * @throws Throwable ignored by the garbage collector
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        finalizeNativeCharacter(characterId);
    }
    // *************************************************************************
    // private methods

    /**
     * Create the configured btKinematicCharacterController.
     */
    private void buildObject() {
        if (objectId == 0L) {
            objectId = createGhostObject();
            assert objectId != 0L;
            logger2.log(Level.FINE, "Creating GhostObject {0}",
                    Long.toHexString(objectId));
            initUserPointer();
        }
        setCharacterFlags(objectId);
        attachCollisionShape(objectId, collisionShape.getObjectId());

        if (characterId != 0L) {
            logger2.log(Level.FINE, "Clearing Character {0}",
                    Long.toHexString(objectId));
            finalizeNativeCharacter(characterId);
        }
        characterId = createCharacterObject(objectId,
                collisionShape.getObjectId(), stepHeight);
        assert characterId != 0L;
        logger2.log(Level.FINE, "Creating Character {0}",
                Long.toHexString(characterId));
    }

    native private long createCharacterObject(long ghostId, long shapeId,
            float stepHeight);

    native private long createGhostObject();

    native private void finalizeNativeCharacter(long characterId);

    native private float getAngularDamping(long characterId);

    native private void getAngularVelocity(long characterId,
            Vector3f storeVector);

    native private float getFallSpeed(long characterId);

    native private void getGravity(long characterId, Vector3f storeVector);

    native private float getJumpSpeed(long characterId);

    native private float getLinearDamping(long characterId);

    native private void getLinearVelocity(long characterId,
            Vector3f storeVector);

    native private float getMaxPenetrationDepth(long characterId);

    native private float getMaxSlope(long characterId);

    native private void getPhysicsLocation(long ghostId, Vector3f storeVector);

    native private void getUpDirection(long characterId, Vector3f storeVector);

    native private void jump(long characterId, Vector3f direction);

    native private boolean onGround(long characterId);

    native private void reset(long characterId, long spaceId);

    native private void setAngularDamping(long characterId, float damping);

    native private void setAngularVelocity(long characterId,
            Vector3f angularVelocity);

    native private void setCharacterFlags(long ghostId);

    native private void setFallSpeed(long characterId, float fallSpeed);

    native private void setGravity(long characterId, Vector3f gravity);

    native private void setJumpSpeed(long characterId, float jumpSpeed);

    native private void setLinearDamping(long characterId, float damping);

    native private void setLinearVelocity(long characterId, Vector3f velocity);

    native private void setMaxPenetrationDepth(long characterId, float depth);

    native private void setMaxSlope(long characterId, float slopeRadians);

    native private void setStepHeight(long characterId, float height);

    native private void setUp(long characterId, Vector3f direction);

    native private void setUseGhostSweepTest(long characterId,
            boolean useGhostSweepTest);

    native private void setWalkDirection(long characterId, Vector3f direction);

    native private void warp(long characterId, Vector3f location);
}
