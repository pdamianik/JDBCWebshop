package at.ac.tgm.insy.sem7.aufgabe2.pdamianik.model;

import javax.persistence.*;

@Entity
@Table(name = "clients")
public class Client {
	@Id @GeneratedValue(generator = "increment")
	private int id;

	private String name;

	private String address;

	private String city;

	private String country;

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
