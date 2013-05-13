package org.jbox2d.testbed.tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.InputMismatchException;
import java.util.Scanner;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Charge;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.MagneticField;
import org.jbox2d.testbed.framework.TestbedTest;

public class Level extends TestbedTest {

	private float xMin;
	private float yMax;
	private float xRes;
	private float yRes;
	private float r;
	private float v_x;
	private float v_y;
	private String levelFile;
	private float positives;
	private float negatives;
	//are you allowed to set the velocity?
	private boolean canSetVelocity;
	
	private static float DENSITY=10;

	public Level(String fileName) {
		super();
		levelFile=fileName;
	}

	@Override
	public boolean isSaveLoadEnabled() {
		return true;
	}

	public void initTest(boolean argDeserialized) {
		if(argDeserialized){
			return;
		}

		getWorld().setGravity(new Vec2(0,0));

		initFromFile(levelFile);
	}

	private Charge createCharge(Vec2 position, BodyType type, float charge) {
		//Make a circle
		CircleShape c2 = new CircleShape();
		c2.setRadius(r);
		//Make a fixture
		FixtureDef fd2 = new FixtureDef();   
		//Put the circle in the fixture and set density
		fd2.shape=c2;
		fd2.density = DENSITY;
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
		//set the charge to be negative
		body2.charge=charge;
		body2.createFixture(fd2);
		return body2;
	}

	private Charge createCharge(Vec2 position, BodyType type, float charge, Vec2 velocity) {
		Charge body2 = createCharge(position,type,charge);
		body2.setLinearVelocity(velocity);
		return body2;
	}
	
	private MagneticField createMagneticField(Vec2 position, float halfHeight, float halfWidth, float strength) {
		//Make a box
	    PolygonShape sd = new PolygonShape();
	    sd.setAsBox(halfWidth, halfHeight);
		//Make a fixture
		FixtureDef fd2 = new FixtureDef();   
		//Put the circle in the fixture and set density
		fd2.shape=sd;
		fd2.filter.categoryBits=0x0008;
		fd2.filter.maskBits=0x0000;//can collide with nothing
		fd2.isSensor=true;//senses when charge is in the magnetic field
		//Make the BodyDef, set its position, and set it as static
		BodyDef bd2 = new BodyDef();
		bd2.position = position;
		bd2.type = BodyType.STATIC;
		//now create a Body in the world, and put the bodydef and the fixturedef into it
		MagneticField body2 = getWorld().createMagneticField(bd2);
		body2.hx=halfWidth;
		body2.hy=halfHeight;
		body2.createFixture(fd2);
		//set the charge to be negative
		body2.setbField(strength);
		return body2;
	}

	private void initFromFile(String s) {
		Scanner scanner = null;
		try {
			File f = new File(s);
			System.out.println(f.getPath());
			scanner = new Scanner(f);
			String line=new String("");
			try {
				System.out.println(scanner.next());
				xMin=scanner.nextFloat();
				System.out.println(scanner.next());
				yMax=scanner.nextFloat();
				System.out.println(scanner.next());
				xRes=scanner.nextFloat();
				System.out.println(scanner.next());
				yRes=scanner.nextFloat();
				System.out.println(scanner.next());
				r=scanner.nextFloat();
				System.out.println(scanner.next());
				v_x=scanner.nextFloat();
				System.out.println(scanner.next());
				v_y=scanner.nextFloat();
				System.out.println(scanner.next());
				setPositives(scanner.nextFloat());
				System.out.println(scanner.next());
				negatives=scanner.nextFloat();
				System.out.println(scanner.next());
				canSetVelocity=scanner.nextBoolean();
				while (line.equals("") || line.charAt(0)=='\\') {
					line = scanner.nextLine();
					System.out.println(line);
				}
			} catch (InputMismatchException e) {
				//Do nothing (incomplete vars)
			}
			int rowCount=0;
			do {
				for(int i=0; i<line.length(); i++) {

					//boolean putNew=false;
					Vec2 position = new Vec2(xMin + i * xRes, yMax- rowCount* yRes);
					System.out.println(position);

					switch (line.charAt(i)) {
					case '+':
						createCharge(position, BodyType.STATIC, 1);
						//putNew=true;
						break;
					case '-':
						createCharge(position, BodyType.STATIC, -1);
						//putNew=true;
						break;
					case 'o':
						createCharge(position, BodyType.DYNAMIC, 1, new Vec2(v_x,v_y));
						//putNew=true;
						break;
					case 'x':
						createMagneticField(position, xRes/2, yRes/2,10);
					case '*':
						// TODO Draw a star! 
						//break;
					case ' ':
						//Do nothing
					}
				}
				line=scanner.nextLine();
				System.out.println(line);
				rowCount++;
			} while (line.charAt(0)!='\\');
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		scanner.close();
	}

	@Override
	public String getTestName() {
		return "Level "+levelFile;
	}

	public float getPositives() {
		return positives;
	}

	public void setPositives(float positives) {
		this.positives = positives;
	}
	
	public void decrementPositives() {
		this.positives--;
	}
	
	public float getNegatives() {
		return negatives;
	}

	public void setNegatives(float negatives) {
		this.negatives = negatives;
	}
	
	public void decrementNegatives() {
		this.negatives--;
	}

	public boolean isCanSetVelocity() {
		return canSetVelocity;
	}

	public void setCanSetVelocity(boolean canSetVelocity) {
		this.canSetVelocity = canSetVelocity;
	}

}
