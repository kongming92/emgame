package org.jbox2d.dynamics;

import org.jbox2d.common.Vec2;

public class Charge extends Body {

	/**
	 * Charge of body
	 */
	public float charge;
	
	public Charge(BodyDef bd, World world) {
		super(bd, world);
		// TODO Auto-generated constructor stub
	}

	public Vec2 forceOn(Body b2) {
		
		//find unit vector from b2 to b
		//now scale by force
		Vec2 v = getPosition().sub(b2.getPosition());
		//now scale: q_1q_2v/|v|^3
		if (v.equals(new Vec2(0,0)) || !(b2 instanceof Charge)) {
			return new Vec2(0,0);
		}
		v = v.mul((float) (-(E_CONSTANT * charge * ((Charge) b2).charge)/Math.pow(v.length(),3)));
		//System.out.println("force: "+v);
		return v;
	}

}
