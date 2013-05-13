/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 	* Redistributions of source code must retain the above copyright notice,
 * 	  this list of conditions and the following disclaimer.
 * 	* Redistributions in binary form must reproduce the above copyright notice,
 * 	  this list of conditions and the following disclaimer in the documentation
 * 	  and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
/**
 * Created at 2:21:03 PM Jul 17, 2010
 */
package org.jbox2d.testbed.framework;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.callbacks.DestructionListener;
import org.jbox2d.callbacks.QueryCallback;
import org.jbox2d.collision.AABB;
import org.jbox2d.collision.Collision;
import org.jbox2d.collision.Collision.PointState;
import org.jbox2d.collision.Manifold;
import org.jbox2d.collision.WorldManifold;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Settings;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Charge;
import org.jbox2d.dynamics.ContactManager;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.Profile;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.serialization.JbDeserializer;
import org.jbox2d.serialization.JbDeserializer.ObjectListener;
import org.jbox2d.serialization.JbSerializer;
import org.jbox2d.serialization.JbSerializer.ObjectSigner;
import org.jbox2d.serialization.SerializationResult;
import org.jbox2d.serialization.UnsupportedListener;
import org.jbox2d.serialization.UnsupportedObjectException;
import org.jbox2d.serialization.pb.PbDeserializer;
import org.jbox2d.serialization.pb.PbSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Murphy
 */
public abstract class TestbedTest
    implements
      ContactListener,
      ObjectListener,
      ObjectSigner,
      UnsupportedListener {
  public static final int MAX_CONTACT_POINTS = 4048;

  protected static final long GROUND_BODY_TAG = 1897450239847L;
  protected static final long BOMB_TAG = 98989788987L;
  protected static final long MOUSE_JOINT_TAG = 4567893364789L;

  private static final Logger log = LoggerFactory.getLogger(TestbedTest.class);

  // keep these static so we don't have to recreate them every time
  public final static ContactPoint[] points = new ContactPoint[MAX_CONTACT_POINTS];
  static {
    for (int i = 0; i < MAX_CONTACT_POINTS; i++) {
      points[i] = new ContactPoint();
    }
  }

  /**
   * Only visible for compatibility. Should use {@link #getWorld()} instead.
   */
  protected World m_world;
  protected Body groundBody;
  
  private Body charge;// ryan
  private final Vec2 chargeSpawnPoint = new Vec2(); // ryan
  private boolean chargeSpawning = false; // ryan

  private final Vec2 mouseWorld = new Vec2();
  private int pointCount;
  private int stepCount;

  private TestbedModel model;
  protected DestructionListener destructionListener;

  private final LinkedList<QueueItem> inputQueue;

  private String title = null;
  protected int m_textLine;
  private final LinkedList<String> textList = new LinkedList<String>();

  private float cachedCameraScale;
  private final Vec2 cachedCameraPos = new Vec2();
  private boolean hasCachedCamera = false;

  private JbSerializer serializer;
  private JbDeserializer deserializer;

  private boolean dialogOnSaveLoadErrors = true;

  private boolean savePending, loadPending, resetPending = false;

  public TestbedTest() {
    inputQueue = new LinkedList<QueueItem>();
    serializer = new PbSerializer(this, new SignerAdapter(this) {
      @Override
      public Long getTag(Body argBody) {
        if (isSaveLoadEnabled()) {
          if (argBody == groundBody) {
            return GROUND_BODY_TAG;
          } 
        }
        return super.getTag(argBody);
      }

      @Override
      public Long getTag(Joint argJoint) {
        if (isSaveLoadEnabled()) {
         
        }
        return super.getTag(argJoint);
      }
    });
    deserializer = new PbDeserializer(this, new ListenerAdapter(this) {
      @Override
      public void processBody(Body argBody, Long argTag) {
        if (isSaveLoadEnabled()) {
          if (argTag == GROUND_BODY_TAG) {
            groundBody = argBody;
            return;
          } 
        }
        super.processBody(argBody, argTag);
      }

      @Override
      public void processJoint(Joint argJoint, Long argTag) {
        if (isSaveLoadEnabled()) {
         
        }
        super.processJoint(argJoint, argTag);
      }
    });
  }

  public void init(TestbedModel argModel) {
    model = argModel;
    destructionListener = new DestructionListener() {

      public void sayGoodbye(Fixture fixture) {}

      public void sayGoodbye(Joint joint) {
        
      }
    };

    m_world = new World(new Vec2(0,0));
    

    BodyDef bodyDef = new BodyDef();
    groundBody = m_world.createBody(bodyDef);

    init(m_world, false);
  }

  public void init(World argWorld, boolean argDeserialized) {
    pointCount = 0;
    stepCount = 0;
    chargeSpawning = false; // ryan

    argWorld.setDestructionListener(destructionListener);
    argWorld.setContactListener(this);
    argWorld.setDebugDraw(model.getDebugDraw());

    if (hasCachedCamera) {
      setCamera(cachedCameraPos, cachedCameraScale);
    } else {
      setCamera(getDefaultCameraPos(), getDefaultCameraScale());
    }
    setTitle(getTestName());

    initTest(argDeserialized);
  }

  /**
   * Gets the current world
   * 
   * @return
   */
  public World getWorld() {
    return m_world;
  }

  /**
   * Gets the testbed model
   * 
   * @return
   */
  public TestbedModel getModel() {
    return model;
  }

  /**
   * Gets the contact points for the current test
   * 
   * @return
   */
  public static ContactPoint[] getContactPoints() {
    return points;
  }

  /**
   * Gets the ground body of the world, used for some joints
   * 
   * @return
   */
  public Body getGroundBody() {
    return groundBody;
  }

  /**
   * Gets the debug draw for the testbed
   * 
   * @return
   */
  public DebugDraw getDebugDraw() {
    return model.getDebugDraw();
  }

  /**
   * Gets the world position of the mouse
   * 
   * @return
   */
  public Vec2 getWorldMouse() {
    return mouseWorld;
  }

  public int getStepCount() {
    return stepCount;
  }

  /**
   * The number of contact points we're storing
   * 
   * @return
   */
  public int getPointCount() {
    return pointCount;
  }



  public float getCachedCameraScale() {
    return cachedCameraScale;
  }

  public void setCachedCameraScale(float cachedCameraScale) {
    this.cachedCameraScale = cachedCameraScale;
  }

  public Vec2 getCachedCameraPos() {
    return cachedCameraPos;
  }

  public void setCachedCameraPos(Vec2 argPos) {
    cachedCameraPos.set(argPos);
  }

  public boolean isHasCachedCamera() {
    return hasCachedCamera;
  }

  public void setHasCachedCamera(boolean hasCachedCamera) {
    this.hasCachedCamera = hasCachedCamera;
  }

  public boolean isDialogOnSaveLoadErrors() {
    return dialogOnSaveLoadErrors;
  }

  public void setDialogOnSaveLoadErrors(boolean dialogOnSaveLoadErrors) {
    this.dialogOnSaveLoadErrors = dialogOnSaveLoadErrors;
  }

  /**
   * Override for a different default camera pos
   * 
   * @return
   */
  public Vec2 getDefaultCameraPos() {
    return new Vec2(0, 10);
  }

  /**
   * Override for a different default camera scale
   * 
   * @return
   */
  public float getDefaultCameraScale() {
    return 10;
  }

  /**
   * Gets the filename of the current test. Default implementation uses the test name with no
   * spaces".
   * 
   * @return
   */
  public String getFilename() {
    return getTestName().toLowerCase().replaceAll(" ", "_") + ".box2d";
  }

  /**
   * Resets the test
   */
  public void reset() {
    resetPending = true;
  }

  /**
   * Saves the test
   */
  public void save() {
    savePending = true;
  }

  /**
   * Loads the test from file
   */
  public void load() {
    loadPending = true;
  }

  protected void _reset() {
    init(model);
  }

  protected void _save() {

    SerializationResult result;
    try {
      result = serializer.serialize(m_world);
    } catch (UnsupportedObjectException e1) {
      log.error("Error serializing world", e1);
      if (dialogOnSaveLoadErrors) {
        JOptionPane.showConfirmDialog(null, "Error serializing the object: " + e1.toString(),
            "Serialization Error", JOptionPane.ERROR_MESSAGE);
      }
      return;
    }

    try {
      FileOutputStream fos = new FileOutputStream(getFilename());
      result.writeTo(fos);
    } catch (FileNotFoundException e) {
      log.error("File not found exception while saving", e);
      if (dialogOnSaveLoadErrors) {
        JOptionPane.showConfirmDialog(null, "File not found exception while saving: "
            + getFilename(), "Serialization Error", JOptionPane.ERROR_MESSAGE);
      }
      return;
    } catch (IOException e) {
      log.error("Exception while writing world", e);
      if (dialogOnSaveLoadErrors) {
        JOptionPane.showConfirmDialog(null, "Error while writing world: " + e.toString(),
            "Serialization Error", JOptionPane.ERROR_MESSAGE);
      }
      return;
    }
    return;
  }

  protected void _load() {

    World w;
    try {
      FileInputStream fis = new FileInputStream(getFilename());
      w = deserializer.deserializeWorld(fis);
    } catch (FileNotFoundException e) {
      log.error("File not found error while loading", e);
      if (dialogOnSaveLoadErrors) {
        JOptionPane.showMessageDialog(null, "File not found exception while loading: "
            + getFilename(), "Serialization Error", JOptionPane.ERROR_MESSAGE);
      }
      return;
    } catch (UnsupportedObjectException e) {
      log.error("Error deserializing world", e);
      if (dialogOnSaveLoadErrors) {
        JOptionPane.showMessageDialog(null, "Error serializing the object: " + e.toString(),
            "Serialization Error", JOptionPane.ERROR_MESSAGE);
      }
      return;
    } catch (IOException e) {
      log.error("Exception while writing world", e);
      if (dialogOnSaveLoadErrors) {
        JOptionPane.showMessageDialog(null, "Error while reading world: " + e.toString(),
            "Serialization Error", JOptionPane.ERROR_MESSAGE);
      }
      return;
    }
    m_world = w;

    init(m_world, true);
    return;
  }

  public void setCamera(Vec2 argPos) {
    model.getDebugDraw().getViewportTranform().setCenter(argPos);
  }

  /**
   * Sets the current testbed camera
   * 
   * @param argPos
   * @param scale
   */
  public void setCamera(Vec2 argPos, float scale) {
    model.getDebugDraw().setCamera(argPos.x, argPos.y, scale);
    hasCachedCamera = true;
    cachedCameraScale = scale;
    cachedCameraPos.set(argPos);
  }

  /**
   * Initializes the current test
   * 
   * @param argDeserialized if the test was deserialized from a file. If so, all physics objects
   *        were already added.
   */
  public abstract void initTest(boolean deserialized);

  /**
   * The name of the test
   * 
   * @return
   */
  public abstract String getTestName();

  /**
   * called when the tests exits
   */
  public void exit() {}

  public void update() {
    if (resetPending) {
      _reset();
      resetPending = false;
    }
    if (savePending) {
      _save();
      savePending = false;
    }
    if (loadPending) {
      _load();
      loadPending = false;
    }

    m_textLine = 20;

    if (title != null) {
      model.getDebugDraw().drawString(model.getPanelWidth() / 2, 15, title, Color3f.WHITE);
      m_textLine += 15;
    }

    // process our input
    if (!inputQueue.isEmpty()) {
      synchronized (inputQueue) {
        while (!inputQueue.isEmpty()) {
          QueueItem i = inputQueue.pop();
          switch (i.type) {
            case QMouse: // ryan
                qMouse(i.p);
                break;
          }
        }
      }
    }

    step(model.getSettings());
  }

  private final Color3f color1 = new Color3f(.3f, .95f, .3f);
  private final Color3f color2 = new Color3f(.3f, .3f, .95f);
  private final Color3f color3 = new Color3f(.9f, .9f, .9f);
  private final Color3f color4 = new Color3f(.6f, .61f, 1);
  private final Color3f color5 = new Color3f(.9f, .9f, .3f);
  private final Color3f mouseColor = new Color3f(0f, 1f, 0f);
  private final Color3f color6 = new Color3f(.9f, .9f, .1f);
  private final Vec2 p1 = new Vec2();
  private final Vec2 p2 = new Vec2();
  private final Vec2 tangent = new Vec2();
  private final List<String> statsList = new ArrayList<String>();

  public synchronized void step(TestbedSettings settings) {
    float hz = settings.getSetting(TestbedSettings.Hz).value;
    float timeStep = hz > 0f ? 1f / hz : 0;
    if (settings.singleStep && !settings.pause) {
      settings.pause = true;
    }

    final DebugDraw debugDraw = model.getDebugDraw();
    if (settings.pause) {
      if (settings.singleStep) {
        settings.singleStep = false;
      } else {
        timeStep = 0;
      }

      debugDraw.drawString(5, m_textLine, "****PAUSED****", Color3f.WHITE);
      m_textLine += 15;
    }

    int flags = 0;
    flags += settings.getSetting(TestbedSettings.DrawShapes).enabled ? DebugDraw.e_shapeBit : 0;
    flags += settings.getSetting(TestbedSettings.DrawJoints).enabled ? DebugDraw.e_jointBit : 0;
    flags += settings.getSetting(TestbedSettings.DrawAABBs).enabled ? DebugDraw.e_aabbBit : 0;
    flags +=
        settings.getSetting(TestbedSettings.DrawCOMs).enabled ? DebugDraw.e_centerOfMassBit : 0;
    flags += settings.getSetting(TestbedSettings.DrawTree).enabled ? DebugDraw.e_dynamicTreeBit : 0;
    debugDraw.setFlags(flags);

    m_world.setAllowSleep(settings.getSetting(TestbedSettings.AllowSleep).enabled);
    m_world.setWarmStarting(settings.getSetting(TestbedSettings.WarmStarting).enabled);
    m_world.setSubStepping(settings.getSetting(TestbedSettings.SubStepping).enabled);
    m_world.setContinuousPhysics(settings.getSetting(TestbedSettings.ContinuousCollision).enabled);

    pointCount = 0;

    m_world.step(timeStep, settings.getSetting(TestbedSettings.VelocityIterations).value,
        settings.getSetting(TestbedSettings.PositionIterations).value);

    m_world.drawDebugData();

    if (timeStep > 0f) {
      ++stepCount;
    }

    if (settings.getSetting(TestbedSettings.DrawStats).enabled) {
      // Vec2.watchCreations = true;
      debugDraw.drawString(5, m_textLine, "Engine Info", color4);
      m_textLine += 15;
      debugDraw.drawString(5, m_textLine, "Framerate: " + model.getCalculatedFps(), Color3f.WHITE);
      m_textLine += 15;
      debugDraw.drawString(
          5,
          m_textLine,
          "bodies/contacts/joints/proxies = " + m_world.getBodyCount() + "/"
              + m_world.getContactCount() + "/" + m_world.getJointCount() + "/"
              + m_world.getProxyCount(), Color3f.WHITE);
      m_textLine += 15;
      debugDraw.drawString(5, m_textLine, "World mouse position: " + mouseWorld.toString(),
          Color3f.WHITE);
      m_textLine += 15;


      statsList.clear();
      Profile p = getWorld().getProfile();
      p.toDebugStrings(statsList);

      for (String s : statsList) {
        debugDraw.drawString(5, m_textLine, s, Color3f.WHITE);
        m_textLine += 15;
      }
      m_textLine += 5;
    }

    if (settings.getSetting(TestbedSettings.DrawHelp).enabled) {
      debugDraw.drawString(5, m_textLine, "Help", color4);
      m_textLine += 15;
      debugDraw.drawString(5, m_textLine, "Click and drag the left mouse button to move objects.",
          Color3f.WHITE);
      m_textLine += 15;
      debugDraw.drawString(5, m_textLine, "Shift-Click to aim a bullet, or press space.",
          Color3f.WHITE);
      m_textLine += 15;
      debugDraw.drawString(5, m_textLine,
          "Click and drag the right mouse button to move the view.", Color3f.WHITE);
      m_textLine += 15;
      debugDraw.drawString(5, m_textLine, "Scroll to zoom in/out.", Color3f.WHITE);
      m_textLine += 15;
      debugDraw.drawString(5, m_textLine, "Press '[' or ']' to change tests, and 'r' to restart.",
          Color3f.WHITE);
      m_textLine += 20;
    }

    if (!textList.isEmpty()) {
      debugDraw.drawString(5, m_textLine, "Test Info", color4);
      m_textLine += 15;
      for (String s : textList) {
        debugDraw.drawString(5, m_textLine, s, Color3f.WHITE);
        m_textLine += 15;
      }
      textList.clear();
    }

    if (settings.getSetting(TestbedSettings.DrawContactPoints).enabled) {
      final float k_impulseScale = 0.1f;
      final float axisScale = 0.3f;

      for (int i = 0; i < pointCount; i++) {

        ContactPoint point = points[i];

        if (point.state == PointState.ADD_STATE) {
          debugDraw.drawPoint(point.position, 10f, color1);
        } else if (point.state == PointState.PERSIST_STATE) {
          debugDraw.drawPoint(point.position, 5f, color2);
        }

        if (settings.getSetting(TestbedSettings.DrawContactNormals).enabled) {
          p1.set(point.position);
          p2.set(point.normal).mulLocal(axisScale).addLocal(p1);
          debugDraw.drawSegment(p1, p2, color3);

        } else if (settings.getSetting(TestbedSettings.DrawContactImpulses).enabled) {
          p1.set(point.position);
          p2.set(point.normal).mulLocal(k_impulseScale).mulLocal(point.normalImpulse).addLocal(p1);
          debugDraw.drawSegment(p1, p2, color5);
        }

        if (settings.getSetting(TestbedSettings.DrawFrictionImpulses).enabled) {
          Vec2.crossToOutUnsafe(point.normal, 1, tangent);
          p1.set(point.position);
          p2.set(tangent).mulLocal(k_impulseScale).mulLocal(point.tangentImpulse).addLocal(p1);
          debugDraw.drawSegment(p1, p2, color5);
        }
      }
    }
    if (ContactManager.win){
      debugDraw.drawString(20, m_textLine+50, "You Win!!!!!",color6);
      settings.pause=true;
      
    }
    
  }
  
  public void queueQMouse(Vec2 p) { // ryan
	    synchronized (inputQueue) {
	      inputQueue.addLast(new QueueItem(QueueItemType.QMouse, p));
	    }
	  }

  public void qMouse(Vec2 p) { //ryan
	    mouseWorld.set(p);
	    synchronized(this) {
	    	chargeSpawnPoint.set(snapWorldPtToGrid(p));
	    	makeCharge(chargeSpawnPoint, vel);
	    }
  }
  

  /**
   * Sets the title of the test
   * 
   * @param argTitle
   */
  public void setTitle(String argTitle) {
    title = argTitle;
  }

  /**
   * Adds a text line to the reporting area
   * 
   * @param argTextLine
   */
  public void addTextLine(String argTextLine) {
    textList.add(argTextLine);
  }

  private final Vec2 vel = new Vec2();

    
  public synchronized void makeCharge(Vec2 position, Vec2 velocity) { // ryan
	    if (charge != null) {
	      m_world.destroyBody(charge);
	      charge = null;
	    }
	    createCharge(position, BodyType.STATIC, 1);
	    // positive
	  }
  
	private Charge createCharge(Vec2 position, BodyType type, float charge) {
		float r=1;
		//Make a circle
		CircleShape c2 = new CircleShape();
		c2.setRadius(r);
		//Make a fixture
		FixtureDef fd2 = new FixtureDef();   
		//Put the circle in the fixture and set density
		fd2.shape=c2;
		fd2.density = 10;
		//Make the BodyDef, set its position, and set it as dynamic
		BodyDef bd2 = new BodyDef();
		bd2.position = position;
		bd2.type = type;
		if (type==BodyType.DYNAMIC) {
			//then it's the player's charge
			fd2.filter.categoryBits=0x0001;
			fd2.filter.maskBits=0x0007;//can collide with walls (4), charges (2), self (1) 
		} else {
			//it's static
			fd2.filter.categoryBits=0x0002;
			fd2.filter.maskBits=0x0001;//can only collide with player (1)
		}
		//now create a Body in the world, and put the bodydef and the fixturedef into it
		Charge body2 = getWorld().createCharge(bd2);
		body2.createFixture(fd2);
		//set the charge to be negative
		body2.charge=charge;
		return body2;
	}

	  public synchronized void spawnCharge(Vec2 worldPt) { // ryan
		worldPt = snapWorldPtToGrid(worldPt);
	    chargeSpawnPoint.set(worldPt);
	    chargeSpawning = true;
	  }

	  /**
	   * Snaps the world point to the nearest grid point
	   * @param worldPt
	   * @return
	   */
	  private Vec2 snapWorldPtToGrid(Vec2 worldPt) {
		  int xRes = 2; // width of a grid rect
		  int yRes = 2; // height of a grid rect
		  Vec2 newPt = new Vec2(worldPt);
		  newPt.x = xRes * Math.round(worldPt.x / (float)xRes);
		  newPt.y = yRes * Math.round(worldPt.y / (float)yRes);
		  return newPt;
	  }

	  
  /**
   * Override to enable saving and loading. Remember to also override the {@link ObjectListener} and
   * {@link ObjectSigner} methods if you need to
   * 
   * @return
   */
  public boolean isSaveLoadEnabled() {
    return false;
  }

  @Override
  public Long getTag(Body body) {
    return null;
  }

  @Override
  public Long getTag(Fixture fixture) {
    return null;
  }

  @Override
  public Long getTag(Joint joint) {
    return null;
  }

  @Override
  public Long getTag(Shape shape) {
    return null;
  }

  @Override
  public Long getTag(World world) {
    return null;
  }

  @Override
  public void processBody(Body body, Long tag) {}

  @Override
  public void processFixture(Fixture fixture, Long tag) {}

  @Override
  public void processJoint(Joint joint, Long tag) {}

  @Override
  public void processShape(Shape shape, Long tag) {}

  @Override
  public void processWorld(World world, Long tag) {}

  @Override
  public boolean isUnsupported(UnsupportedObjectException exception) {
    return true;
  }

  public void jointDestroyed(Joint joint) {}

  public void beginContact(Contact contact) {}

  public void endContact(Contact contact) {}

  public void postSolve(Contact contact, ContactImpulse impulse) {}

  private final PointState[] state1 = new PointState[Settings.maxManifoldPoints];
  private final PointState[] state2 = new PointState[Settings.maxManifoldPoints];
  private final WorldManifold worldManifold = new WorldManifold();

  public void preSolve(Contact contact, Manifold oldManifold) {
    Manifold manifold = contact.getManifold();

    if (manifold.pointCount == 0) {
      return;
    }

    Fixture fixtureA = contact.getFixtureA();
    Fixture fixtureB = contact.getFixtureB();

    Collision.getPointStates(state1, state2, oldManifold, manifold);

    contact.getWorldManifold(worldManifold);

    for (int i = 0; i < manifold.pointCount && pointCount < MAX_CONTACT_POINTS; i++) {
      ContactPoint cp = points[pointCount];
      cp.fixtureA = fixtureA;
      cp.fixtureB = fixtureB;
      cp.position.set(worldManifold.points[i]);
      cp.normal.set(worldManifold.normal);
      cp.state = state2[i];
      cp.normalImpulse = manifold.points[i].normalImpulse;
      cp.tangentImpulse = manifold.points[i].tangentImpulse;
      ++pointCount;
    }
  }

  public void keyPressed(char keyCar, int keyCode) {}

  public void keyReleased(char keyChar, int keyCode) {}
}


class TestQueryCallback implements QueryCallback {

  public final Vec2 point;
  public Fixture fixture;

  public TestQueryCallback() {
    point = new Vec2();
    fixture = null;
  }

  public boolean reportFixture(Fixture argFixture) {
    Body body = argFixture.getBody();
    if (body.getType() == BodyType.DYNAMIC) {
      boolean inside = argFixture.testPoint(point);
      if (inside) {
        fixture = argFixture;

        return false;
      }
    }

    return true;
  }
}


enum QueueItemType {
	QMouse
}


class QueueItem {
  public QueueItemType type;
  public Vec2 p;
  public char c;
  public int code;

  public QueueItem(QueueItemType t, Vec2 pt) {
    type = t;
    p = pt;
  }

  public QueueItem(QueueItemType t, char cr, int cd) {
    type = t;
    c = cr;
    code = cd;
  }
}


class SignerAdapter implements ObjectSigner {
  private final ObjectSigner delegate;

  public SignerAdapter(ObjectSigner argDelegate) {
    delegate = argDelegate;
  }

  public Long getTag(World argWorld) {
    return delegate.getTag(argWorld);
  }

  public Long getTag(Body argBody) {
    return delegate.getTag(argBody);
  }

  public Long getTag(Shape argShape) {
    return delegate.getTag(argShape);
  }

  public Long getTag(Fixture argFixture) {
    return delegate.getTag(argFixture);
  }

  public Long getTag(Joint argJoint) {
    return delegate.getTag(argJoint);
  }
}


class ListenerAdapter implements ObjectListener {
  private final ObjectListener listener;

  public ListenerAdapter(ObjectListener argListener) {
    listener = argListener;
  }

  public void processWorld(World argWorld, Long argTag) {
    listener.processWorld(argWorld, argTag);
  }

  public void processBody(Body argBody, Long argTag) {
    listener.processBody(argBody, argTag);
  }

  public void processFixture(Fixture argFixture, Long argTag) {
    listener.processFixture(argFixture, argTag);
  }

  public void processShape(Shape argShape, Long argTag) {
    listener.processShape(argShape, argTag);
  }

  public void processJoint(Joint argJoint, Long argTag) {
    listener.processJoint(argJoint, argTag);
  }
}
