package org.jbox2d.dynamics;

import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.Charge;
import org.jbox2d.dynamics.World;



public class Star extends Body{
   
  public Star(BodyDef bd, World world) {
    super(bd, world);
    
  }
  public boolean hit(Charge c){
    if (this.getPosition()==c.getPosition()){
      return true;
    }
    else{
      return false;
    }
  }
  
}

