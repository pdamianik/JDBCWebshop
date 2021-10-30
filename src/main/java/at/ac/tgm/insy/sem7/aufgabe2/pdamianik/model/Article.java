package at.ac.tgm.insy.sem7.aufgabe2.pdamianik.model;

import javax.persistence.*;

@Entity
@Table(name = "articles")
public class Article {
	@Id @GeneratedValue(generator = "increment")
	private int id;

	private String description;

	private int price;

	@Column(name = "amount")
	private int amountAvailable;

	public int getAmountAvailable() {
		return amountAvailable;
	}

	public void setAmountAvailable(int amountAvailable) {
		this.amountAvailable = amountAvailable;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
