/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package jme3test.games;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.DirectionalLightShadowFilter;
import java.util.concurrent.Callable;

/**
 * Physics based marble game.
 *
 * @author SkidRunner (Mark E. Picknell)
 */
public class RollingTheMonkey extends SimpleApplication implements ActionListener, PhysicsCollisionListener {

    private static final String MESSAGE         = "Thanks for Playing!";
    private static final String INFO_MESSAGE    = "Collect all the spinning cubes!\nPress the 'R' key any time to reset!";

    private static final float PLAYER_DENSITY   = 1200;  // OLK(Java LOL) = 1200, STEEL = 8000, RUBBER = 1000
    private static final float PLAYER_REST      = 0.1f;     // OLK = 0.1f, STEEL = 0.0f, RUBBER = 1.0f I made these up.

    private static final float PLAYER_RADIUS    = 2.0f;
    private static final float PLAYER_ACCEL     = 5.0f;    //private static final float PLAYER_ACCEL     = 1.0f;//original value

    private static final float PICKUP_SIZE      = .5f;//0.5
    private static final float PICKUP_RADIUS    = 15.0f;
    private static final int   PICKUP_COUNT     = 16;//16
    private static final float PICKUP_SPEED     = 5.0f;//50 or 5

    private static final float PLAYER_VOLUME    = (FastMath.pow(PLAYER_RADIUS, 3) * FastMath.PI) / 3;   // V = 4/3 * PI * R pow 3
    private static final float PLAYER_MASS      = PLAYER_DENSITY * PLAYER_VOLUME;
    private static final float PLAYER_FORCE     = 80000 * PLAYER_ACCEL;  // F = M(4m diameter steel ball) * A
    private static final Vector3f PLAYER_START  = new Vector3f(0.0f, PLAYER_RADIUS * 2, 0.0f);

    private static final String INPUT_MAPPING_FORWARD   = "INPUT_MAPPING_FORWARD";
    private static final String INPUT_MAPPING_BACKWARD  = "INPUT_MAPPING_BACKWARD";
    private static final String INPUT_MAPPING_LEFT      = "INPUT_MAPPING_LEFT";
    private static final String INPUT_MAPPING_RIGHT     = "INPUT_MAPPING_RIGHT";
    private static final String INPUT_MAPPING_RESET     = "INPUT_MAPPING_RESET";

    public static void main(String[] args) {
        RollingTheMonkey app = new RollingTheMonkey(); //an object of the same type we are in called app is created
        app.start(); //the method start is called by app. Start is either in the app class or its superclass
    }

    private boolean keyForward; //this boolean variable is true when the up arrow key is pressed and false otherwise
    private boolean keyBackward; //this boolean variable is true when the down arrow key is pressed and false otherwise
    private boolean keyLeft; //this boolean variable is true when the left arrow key is pressed and false otherwise
    private boolean keyRight; //this boolean variable is true when the right arrow key is pressed and false otherwise

    private PhysicsSpace space; //a private object called space of type PhysicsSpace is declared

    private RigidBodyControl player; //a private object called player of type RigidBodyControl is declared
    private int score; //a private integer variable called score is declared

    private Node pickUps; //a private node called pickUps is declared

    private BitmapText infoText; //a private BitmapText object called infoText is declared
    private BitmapText scoreText; //a private BitmapText object called scoreText is declared
    private BitmapText messageText; //a private BitmapText object called messageText is declared
    //the Bitmaptext class is used to make Bitmap fonts

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false); //the FlyBy Camera is set to false and ignores arrow key input
        cam.setLocation(new Vector3f(0.0f, 12.0f, 21.0f)); //sets the camera location at a point in 3D space
        viewPort.setBackgroundColor(new ColorRGBA(0.2118f, 0.0824f, 0.6549f, 1.0f)); //sets the color of the background based on RGB

        // init physics
        BulletAppState bulletState = new BulletAppState(); //an object of BulletAppState type that manages the physics and collisions
        stateManager.attach(bulletState); //the physics state is attached to the state manager
        space = bulletState.getPhysicsSpace(); //the physics state is stored in the physical space
        space.addCollisionListener(this); //the physical space will now understand collisions

        // create light
        DirectionalLight sun = new DirectionalLight(); //a DirectionalLight object called sun is declared and instantiated
        sun.setDirection((new Vector3f(-0.7f, -0.3f, -0.5f)).normalizeLocal()); //the direction of the directional light called sun is set
        System.out.println("Here We Go: " + sun.getDirection()); //a  message about the sun's direction is printed to the console
        sun.setColor(ColorRGBA.White); //the color of the sun is set
        rootNode.addLight(sun); //sun is added as a node to the Spatial

        // create materials
        Material materialRed = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md"); // an object of Material type is created and specifies what j3md file to use
        materialRed.setBoolean("UseMaterialColors",true); //activates material colors
        materialRed.setBoolean("HardwareShadows", true);  //activates shadows
        materialRed.setColor("Diffuse", new ColorRGBA(0.9451f, 0.0078f, 0.0314f, 1.0f)); //sets the color of the diffuse
        materialRed.setColor("Specular", ColorRGBA.White); //sets the color of the specular
        materialRed.setFloat("Shininess", 64.0f); //sets the value of the shininess

        Material materialGreen = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        materialGreen.setBoolean("UseMaterialColors",true);
        materialGreen.setBoolean("HardwareShadows", true);
        materialGreen.setColor("Diffuse", new ColorRGBA(0.0431f, 0.7725f, 0.0078f, 1.0f));
        materialGreen.setColor("Specular", ColorRGBA.White);
        materialGreen.setFloat("Shininess", 64.0f);

        Material logoMaterial = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md"); //an object of type Material is created and specifies what j3md file is used
        logoMaterial.setBoolean("UseMaterialColors",true);
        logoMaterial.setBoolean("HardwareShadows", true);
        logoMaterial.setTexture("DiffuseMap", assetManager.loadTexture("com/jme3/app/Monkey.png")); //texture parameters are set
        logoMaterial.setColor("Diffuse", ColorRGBA.White);
        logoMaterial.setColor("Specular", ColorRGBA.White);
        logoMaterial.setFloat("Shininess", 32.0f);

        Material materialYellow = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        materialYellow.setBoolean("UseMaterialColors",true);
        materialYellow.setBoolean("HardwareShadows", true);
        materialYellow.setColor("Diffuse", new ColorRGBA(0.9529f, 0.7843f, 0.0078f, 1.0f));
        materialYellow.setColor("Specular", ColorRGBA.White);
        materialYellow.setFloat("Shininess", 64.0f);

        // create level spatial
        // TODO: create your own level mesh
        Node level = new Node("level"); //an object of type Node called level is created
        level.setShadowMode(ShadowMode.CastAndReceive); //shadow interactions are activated for level

        //Geometry floor = new Geometry("floor", new Box(22.0f, 0.5f, 22.0f)); //an object of type Geometry called floor, a node, is created
       Geometry floor = new Geometry("floor", new Sphere(8, 8, 20f)); //an object of type Geometry called floor, a node, is created
        floor.setShadowMode(ShadowMode.Receive); //floor will only receive shadows
        floor.setLocalTranslation(0.0f, -20f, 0.0f); //the local translation is set
        floor.setMaterial(materialGreen); //floor is given a green material

        Geometry wallNorth = new Geometry("wallNorth", new Box(22.0f, 2.0f, 0.5f)); //an object of type Geometry called wallNorth, a node, is created
        wallNorth.setLocalTranslation(0.0f, 2.0f, 21.5f);
        wallNorth.setMaterial(materialRed);

        Geometry wallSouth = new Geometry("wallSouth", new Box(22.0f, 2.0f, 0.5f));
        wallSouth.setLocalTranslation(0.0f, 2.0f, -21.5f);
        wallSouth.setMaterial(materialRed);

        Geometry wallEast = new Geometry("wallEast", new Box(0.5f, 2.0f, 21.0f));
        wallEast.setLocalTranslation(-21.5f, 2.0f, 0.0f);
        wallEast.setMaterial(materialRed);

        Geometry wallWest = new Geometry("wallWest", new Box(0.5f, 2.0f, 21.0f));
        wallWest.setLocalTranslation(21.5f, 2.0f, 0.0f);
        wallWest.setMaterial(materialRed);

        //floor, wallNorth, wallSouth, wallEast, wallWest are all attached to level as children
        level.attachChild(floor);
        level.attachChild(wallNorth);
        level.attachChild(wallSouth);
        level.attachChild(wallEast);
        level.attachChild(wallWest);

        // The easy way: level.addControl(new RigidBodyControl(0));

        // create level Shape
        CompoundCollisionShape levelShape = new CompoundCollisionShape(); //a custom collision shape called levelShape is created
        BoxCollisionShape floorShape = new BoxCollisionShape(new Vector3f(22.0f, 0.5f, 22.0f)); //a box collision shape called floorShape is created and its size is set
        //SphereCollisionShape floorShape = new SphereCollisionShape(200);
        BoxCollisionShape wallNorthShape = new BoxCollisionShape(new Vector3f(22.0f, 2.0f, 0.5f));
        BoxCollisionShape wallSouthShape = new BoxCollisionShape(new Vector3f(22.0f, 2.0f, 0.5f));
        BoxCollisionShape wallEastShape = new BoxCollisionShape(new Vector3f(0.5f, 2.0f, 21.0f));
        BoxCollisionShape wallWestShape = new BoxCollisionShape(new Vector3f(0.5f, 2.0f, 21.0f));

        levelShape.addChildShape(floorShape, new Vector3f(0.0f, -0.5f, 0.0f)); //the box collision shapes from above are attached to levelShape as children at a given location
        levelShape.addChildShape(wallNorthShape, new Vector3f(0.0f, 2.0f, -21.5f));
        levelShape.addChildShape(wallSouthShape, new Vector3f(0.0f, 2.0f, 21.5f));
        levelShape.addChildShape(wallEastShape, new Vector3f(-21.5f, 2.0f, 0.0f));
        levelShape.addChildShape(wallWestShape, new Vector3f(21.5f, 2.0f, 0.0f));

        level.addControl(new RigidBodyControl(levelShape, 0)); //level is given RigidBodyControl of levelShape

        rootNode.attachChild(level); //level is added as a node to the rootNode in Simple Application
        space.addAll(level); //physics controls and joints are added to level

        // create Pickups ?
        // TODO: create your own pickUp mesh
        //       create single mesh for all pickups
        // HINT: think particles.
        pickUps = new Node("pickups");

        Quaternion rotation = new Quaternion(); //an object of type Quaternion called rotation is created
        Vector3f translation = new Vector3f(0.0f, PICKUP_SIZE * 1.5f, -PICKUP_RADIUS); //an object of type Vector3f called translation is created?
        int index = 0; //a variable of type int called index is declared and initialized to 0
        float ammount = FastMath.TWO_PI / PICKUP_COUNT; //a variable of type float called ammount is declared and initialized to the result of the approximation of two pi divided by the pickup count
        for(float angle = 0; angle < FastMath.TWO_PI; angle += ammount) { //a variable of type float called angle is initialized to 0. checks if angle is less than 2 PI. Angle is incremented by ammount
            Geometry pickUp = new Geometry("pickUp" + (index++), new Box(PICKUP_SIZE,PICKUP_SIZE, PICKUP_SIZE)); //an object of type geometry called pickup is declared and instantiated
            pickUp.setShadowMode(ShadowMode.CastAndReceive); //pickUp is given the ability to cast and receive shadows
            pickUp.setMaterial(materialYellow); //pickUp is given a yellow material
            pickUp.setLocalRotation(rotation.fromAngles(
                    FastMath.rand.nextFloat() * FastMath.TWO_PI,
                    FastMath.rand.nextFloat() * FastMath.TWO_PI,
                    FastMath.rand.nextFloat() * FastMath.TWO_PI)); //various angles of rotation are given to pickUp

            rotation.fromAngles(0.0f, angle, 0.0f); //rotation is given angles of rotation
            rotation.mult(translation, pickUp.getLocalTranslation()); //creating a new vector from the quaternion
            pickUps.attachChild(pickUp); //pickUP is attached to pickUps as a child node

            pickUp.addControl(new GhostControl(new SphereCollisionShape(PICKUP_SIZE))); //the control of a sphere collision shape is given to pickUp


            space.addAll(pickUp); //pickUp is added to the physics space
            //space.addCollisionListener(pickUpControl);
        }
        rootNode.attachChild(pickUps); //pickUps is made a child node of a node in Simple Application

        // Create player
        // TODO: create your own player mesh
        Geometry playerGeometry = new Geometry("player", new Sphere(5, 5, 2*PLAYER_RADIUS)); // an object of type Geometry called playerGeometry is declared and instantiated
        playerGeometry.setShadowMode(ShadowMode.CastAndReceive); //playerGeometry can cast and receive shadows
        playerGeometry.setLocalTranslation(PLAYER_START.clone()); //the local translation of playerGeemetry is set
        playerGeometry.setMaterial(logoMaterial); //the material of playerGeometry is set

        // Store control for applying forces
        // TODO: create your own player control
        player = new RigidBodyControl(new SphereCollisionShape(2*PLAYER_RADIUS), PLAYER_MASS); //the rigid body control of a sphere collision shape is given to player
        player.setRestitution(PLAYER_REST); //the bounciness of the player is set

        playerGeometry.addControl(player); //control of player is given to playerGeometry

        rootNode.attachChild(playerGeometry); //playerGeometry is attached as a node to the root node in Simple Application
        space.addAll(playerGeometry); //playerGeometry is added to the physics space

        inputManager.addMapping(INPUT_MAPPING_FORWARD, new KeyTrigger(KeyInput.KEY_UP)
                , new KeyTrigger(KeyInput.KEY_W)); //movement forward is mapped to the up arrow and the w key
        inputManager.addMapping(INPUT_MAPPING_BACKWARD, new KeyTrigger(KeyInput.KEY_DOWN)
                , new KeyTrigger(KeyInput.KEY_S)); //movement backward is mapped to the down arrow and the s key
        inputManager.addMapping(INPUT_MAPPING_LEFT, new KeyTrigger(KeyInput.KEY_LEFT)
                , new KeyTrigger(KeyInput.KEY_A)); //movement left is mapped to the left arrow and the a key
        inputManager.addMapping(INPUT_MAPPING_RIGHT, new KeyTrigger(KeyInput.KEY_RIGHT)
                , new KeyTrigger(KeyInput.KEY_D)); //movement right is mapped to the right arrow and the d key
        inputManager.addMapping(INPUT_MAPPING_RESET, new KeyTrigger(KeyInput.KEY_R)); //resetting the game is mapped to the r key
        inputManager.addListener(this, INPUT_MAPPING_FORWARD, INPUT_MAPPING_BACKWARD
                , INPUT_MAPPING_LEFT, INPUT_MAPPING_RIGHT, INPUT_MAPPING_RESET); //listens for inputs

        // init UI
        infoText = new BitmapText(guiFont, false); //a bitmap font called infoText is created
        infoText.setText(INFO_MESSAGE); //the text for the infoText is set
        guiNode.attachChild(infoText); //infoText is attached to guiNode as a child node

        scoreText = new BitmapText(guiFont, false); //a bitmap font called scoreText is created
        scoreText.setText("Score: 0"); //the text for scoreText is set
        guiNode.attachChild(scoreText); //scoreText is attached to guiNode as a child node

        messageText = new BitmapText(guiFont, false); //a bitmap font called messageText is created
        messageText.setText(MESSAGE); //the text for messageText is set
        messageText.setLocalScale(0.0f); //the scale of messageText is set
        guiNode.attachChild(messageText); //messageText is attacshed to guiNode as a child node

        infoText.setLocalTranslation(0.0f, cam.getHeight(), 0.0f); //the local translation of infoText is set
        scoreText.setLocalTranslation((cam.getWidth() - scoreText.getLineWidth()) / 2.0f,
                scoreText.getLineHeight(), 0.0f); //the local translation of scoreText is set
        messageText.setLocalTranslation((cam.getWidth() - messageText.getLineWidth()) / 2.0f,
                (cam.getHeight() - messageText.getLineHeight()) / 2, 0.0f); //the local translation of messageText is set

        // init shadows
        FilterPostProcessor processor = new FilterPostProcessor(assetManager); //an object of type FilterPostProcessor called processor is declared and initialized. This object manages filters applied to the scene.
        DirectionalLightShadowFilter filter = new DirectionalLightShadowFilter(assetManager, 2048, 1); //an object of type DirectionalLightShadowFilter called filter is declared and initialized
        filter.setLight(sun); //the type of light that filter would use is set to sun
        processor.addFilter(filter); //filter is added to processor
        viewPort.addProcessor(processor); //processor is added to viewPort

    }

    @Override
    public void simpleUpdate(float tpf) {
        // Update and position the score
        scoreText.setText("Score: " + score); //the text that displays the score is being updated based on the score you obtain
        scoreText.setLocalTranslation((cam.getWidth() - scoreText.getLineWidth()) / 2.0f,
                scoreText.getLineHeight(), 0.0f); //the location of scoreText is set based on the width and height of the line in scoreText?

        // Rotate all the pickups
        float pickUpSpeed = PICKUP_SPEED * tpf; //the constant speed of the pickups is set
        for(Spatial pickUp : pickUps.getChildren()) { //a Spatial called pickUp is being declared. Every time we make a pickUp, we return all the children to pickUps.
            pickUp.rotate(pickUpSpeed, pickUpSpeed, pickUpSpeed); //pickUp is rotated in all directions
        }

        Vector3f centralForce = new Vector3f(); //an object of type Vector3f called centralForce is declared and initialized to 0

        //the input from the user triggers the retrieval of a positive or negative vector based on the direction of the camera
        if(keyForward) centralForce.addLocal(cam.getDirection());
        if(keyBackward) centralForce.addLocal(cam.getDirection().negate());
        if(keyLeft) centralForce.addLocal(cam.getLeft());
        if(keyRight) centralForce.addLocal(cam.getLeft().negate());

        if(!Vector3f.ZERO.equals(centralForce)) {
            centralForce.setY(0);                   // stop ball from pushing down or flying up
            centralForce.normalizeLocal();          // normalize force
            centralForce.multLocal(PLAYER_FORCE);   // scale vector to force

            player.applyCentralForce(centralForce); // apply force to player
        }

        cam.lookAt(player.getPhysicsLocation(), Vector3f.UNIT_Y); //the camera follows player based on player's location
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        switch(name) { //the case that is checked is based on what the input the user presses
            case INPUT_MAPPING_FORWARD:
                keyForward = isPressed; //if the user presses the up arrow or the w key, the keyForward variable is true. This applies a force to player in simpleUpdate
                break;
            case INPUT_MAPPING_BACKWARD:
                keyBackward = isPressed;
                break;
            case INPUT_MAPPING_LEFT:
                keyLeft = isPressed;
                break;
            case INPUT_MAPPING_RIGHT:
                keyRight = isPressed;
                break;
            case INPUT_MAPPING_RESET:
                enqueue(new Callable<Void>() {
                    @Override
                    public Void call() {
                        reset();
                        return null;
                    }
                });
                break;
        }
    }
    @Override
    public void collision(PhysicsCollisionEvent event) {
        //this method receives an object that stores all the physical collision information
        Spatial nodeA = event.getNodeA(); //The Spatial called nodeA is being set as the value of NodeA in event
        Spatial nodeB = event.getNodeB(); //The Spatial called nodeB is being set as the value of NodeB in event

        String nameA = nodeA == null ? "" : nodeA.getName(); //checks to see if nodeA has a name
        String nameB = nodeB == null ? "" : nodeB.getName(); //checks to see if nodeB has a name

        if(nameA.equals("player") && nameB.startsWith("pickUp")) { //checks to see if the labels of the nodes are what they are supposed to be
            GhostControl pickUpControl = nodeB.getControl(GhostControl.class); //a GhostControl called pickUpControl is being set to the value of a GhostControl in nodeB
            if(pickUpControl != null && pickUpControl.isEnabled()) { //checks to see if pickUpControl is not null and pickUpControl is enabled
                pickUpControl.setEnabled(false); //pickUpControl is set to false
                nodeB.removeFromParent(); //nodeB is removed from its parent node
                nodeB.setLocalScale(0.0f); //nodeB is scaled locally
                score += 1; //score is incremented by 1
                if(score >= PICKUP_COUNT) { //checks if the score is greater than or equal to pickup count
                    messageText.setLocalScale(1.0f); //messageText is scaled locally
                }
            }
        } else if(nameA.startsWith("pickUp") && nameB.equals("player")) { //checks to see if the labels of the nodes are what they are supposed to be
            GhostControl pickUpControl = nodeA.getControl(GhostControl.class); //a GhostControl called pickUpControl is being set to the value of a GhostControl in nodeA
            if(pickUpControl != null && pickUpControl.isEnabled()) { //checks to see if pickUpControl is not null and pickUpControl is enabled
                pickUpControl.setEnabled(false); //pickUpControl is set to false
                nodeA.setLocalScale(0.0f); //nodeA is scaled locally
                score += 1; //score is incremented by 1
                if(score >= PICKUP_COUNT) { //checks if the score is greater than or equal to pickup count
                    messageText.setLocalScale(1.0f); //messageText is scaled locally
                }
            }
        }
    }

    private void reset() {
        // Reset the pickups
        for(Spatial pickUp : pickUps.getChildren()) {  //a Spatial called pickUp is being declared. Every time we make a pickUp, we return all the children to pickUps.
            GhostControl pickUpControl = pickUp.getControl(GhostControl.class); //a GhostControl called pickUpControl is being set to the value of a GhostControl in pickUp
            if(pickUpControl != null) { //if pickUpControl is not null, the ghost object remains in the physics space
                pickUpControl.setEnabled(true);
            }
            pickUp.setLocalScale(1.0f); //the pickUp is scaled locally
        }
        // Reset the player
        player.setPhysicsLocation(PLAYER_START.clone()); //the location of player is reset
        player.setAngularVelocity(Vector3f.ZERO.clone()); //the angular velocity of player is reset
        player.setLinearVelocity(Vector3f.ZERO.clone()); //the linear velocity of player is reset
        // Reset the score
        score = 0; //score is set to 0
        // Reset the message
        messageText.setLocalScale(0.0f); //messageText is scaled locally
    }

}