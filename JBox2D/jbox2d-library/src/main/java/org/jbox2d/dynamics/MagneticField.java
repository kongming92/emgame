package org.jbox2d.dynamics;

import org.jbox2d.common.Vec2;

public class MagneticField extends Body {

	/**
	 * positive for pointing out of page.
	 */
	private float bField;
	//this is not good implementation but I can't figure out how Contacts and ContactListeners work
	public float hx;
	public float hy;
	
	public MagneticField(BodyDef bd, World world) {
		super(bd, world);
	}
	
	public MagneticField(BodyDef bd, World world, float x,float y) {
		super(bd, world);
		hx=x;
		hy=y;
	}
	
	public float getbField() {
		return bField;
	}

	public void setbField(float bField) {
		this.bField = bField;
	}
	
	public Vec2 forceOn(Body b2) {
		
		//find unit vector from b2 to b
		//now scale by force
		//Vec2 v = getPosition().sub(b2.getPosition());
		//now scale: q_1q_2v/|v|^3
		if ((!(b2 instanceof Charge))||(!(contains(b2)))) {
			//System.out.println("No");
			return new Vec2(0,0);
		}
		Vec2 v = b2.getLinearVelocity();
		Vec2 v_perp = new Vec2(v.y,-v.x);
		v_perp = v_perp.mul(bField*World.SPEED);
		//System.out.println("force: "+v);
		//System.out.println(v_perp);
		return v_perp;
	}
	
	public boolean contains(Body b) {
		Vec2 p = b.getPosition();
		return ((p.x>=getPosition().x-hx)&&(p.x<=getPosition().x+hx)&& (p.y>=getPosition().y-hy)&& (p.y<=getPosition().y+hy));
	}
	
	public void flip() { 
		bField=-bField;
	}
	
}
