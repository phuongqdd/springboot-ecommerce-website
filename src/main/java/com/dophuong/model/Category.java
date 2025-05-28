package com.dophuong.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Id;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Category {

 	@Id
 	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	
 	private String name;
 	private String imageName;
 	
 	private Boolean isActive;

}
