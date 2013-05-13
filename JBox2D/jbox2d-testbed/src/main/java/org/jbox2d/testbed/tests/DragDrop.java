package org.jbox2d.testbed.tests;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.testbed.framework.TestbedTest;

public class DragDrop extends TestbedTest {

	@Override
	public boolean isSaveLoadEnabled() {
		return true;
	}

	public void initTest(boolean argDeserialized) {
		if(argDeserialized){
			return;
		}

		getWorld().setGravity(new Vec2(0,0));
		
		float density=25;
		
		//Make a circle
		CircleShape c = new CircleShape();
		c.setRadius(1);
		//Make a fixture
		FixtureDef fd = new FixtureDef();   
		//Put the circle in the fixture and set density
		fd.shape=c;
		fd.density = density;
		//Make the BodyDef, set its position, and set it as dynamic
		BodyDef bd = new BodyDef();
		bd.position = new Vec2(0.0f, 0.0f);
		bd.type = BodyType.DYNAMIC;
		//now create a Body in the world, and put the bodydef and the fixturedef into it
		Body body = getWorld().createBody(bd);
		body.createFixture(fd);
		//set the charge to be positive
		body.charge=1;
	    

		//Make a circle
		CircleShape c2 = new CircleShape();
		c2.setRadius(1);
		//Make a fixture
		FixtureDef fd2 = new FixtureDef();   
		//Put the circle in the fixture and set density
		fd2.shape=c2;
		fd2.density = density;
		//Make the BodyDef, set its position, and set it as dynamic
		BodyDef bd2 = new BodyDef();
		bd2.position = new Vec2(10.0f, 10.0f);
		bd2.type = BodyType.DYNAMIC;
		//now create a Body in the world, and put the bodydef and the fixturedef into it
		Body body2 = getWorld().createBody(bd2);
		body2.createFixture(fd2);
		//set the charge to be negative
		body2.charge=-1;
		
		/*
		BodyDef bd2 = new BodyDef();
		bd2.position = new Vec2(10.0f,10.0f);
		bd.type = BodyType.DYNAMIC;
		//now create a Body in the world, and put the bodydef and the fixturedef into it
		Body body2 = getWorld().createBody(bd);
		body2.createFixture(fd);//okay to reuse fixtures
		//set the charge
		body2.charge=-1;
		*/
		//body.applyForce(new Vec2(1,1), new Vec2(0,0));
		
	}

	@Override
	public String getTestName() {
		return "Charges";
	}
}
