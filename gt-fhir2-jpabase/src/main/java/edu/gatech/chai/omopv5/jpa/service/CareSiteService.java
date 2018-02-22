package edu.gatech.chai.omopv5.jpa.service;

import java.util.List;

import edu.gatech.chai.omopv5.jpa.entity.CareSite;
import edu.gatech.chai.omopv5.jpa.entity.Location;

public interface CareSiteService extends IService<CareSite> {
	public CareSite searchByLocation(Location location);
	public CareSite searchByNameAndLocation(String careSiteName, Location location);
}
