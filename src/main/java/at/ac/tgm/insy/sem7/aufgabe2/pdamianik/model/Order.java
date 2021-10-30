package at.ac.tgm.insy.sem7.aufgabe2.pdamianik.model;

import java.util.Date;
import java.util.Set;

public class Order {
	private Client client;
	private Date createdAt;
	private int id;
	private Set<OrderLine> orderLines;

	public Set<OrderLine> getOrderLines() {
		return orderLines;
	}

	public void setOrderLines(Set<OrderLine> orderLines) {
		this.orderLines = orderLines;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}
}
