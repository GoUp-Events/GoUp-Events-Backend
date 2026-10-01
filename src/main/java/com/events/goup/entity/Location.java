package com.events.goup.entity;

import com.events.goup.entity.enums.PriceLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_id", nullable = false, unique = true, length = 255)
    private String placeId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "formatted_address", nullable = false, length = 255)
    private String formattedAddress;

    @Column(length = 200)
    private String address;

    @Column(length = 20)
    private String number;

    @Column(length = 100)
    private String neighborhood;

    @Column(length = 100)
    private String city;

    @Column(length = 2)
    private String state;

    @Column(length = 9)
    private String zip;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private Double rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_level", length = 20)
    private PriceLevel priceLevel;
}
