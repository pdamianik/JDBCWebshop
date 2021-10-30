package at.ac.tgm.insy.sem7.aufgabe2.pdamianik.model;

import javax.persistence.*;

@Entity
@Table(name = "order_lines")
public class OrderLine {
	@Id @GeneratedValue(generator = "increment")
	private int id;

	private int amount;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Article article;

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Article getArticle() {
		return article;
	}

	public void setArticle(Article article) {
		this.article = article;
	}
}
