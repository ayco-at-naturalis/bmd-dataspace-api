package eu.bmdproject.dataspace.model;

import java.time.OffsetDateTime;

public final class DefaultSite extends Site {
  private OffsetDateTime dateCompilation;
  private OffsetDateTime dateUpdate;
  private OffsetDateTime dateSpa;
  private String spaLegalReference;
  private OffsetDateTime datePropSci;
  private OffsetDateTime dateConfSci;
  private OffsetDateTime dateSac;
  private String sacLegalReference;
  private String explanations;
  private Double lengthKm;
  private Double latitude;
  private Double longitude;
  private Double marineAreaPercentage;
  private String documentation;
  private String quality;
  private String designation;
  private String othercharact;
  private String inspireId;

  public OffsetDateTime getDateCompilation() {
    return dateCompilation;
  }

  public void setDateCompilation(OffsetDateTime dateCompilation) {
    this.dateCompilation = dateCompilation;
  }

  public OffsetDateTime getDateUpdate() {
    return dateUpdate;
  }

  public void setDateUpdate(OffsetDateTime dateUpdate) {
    this.dateUpdate = dateUpdate;
  }

  public OffsetDateTime getDateSpa() {
    return dateSpa;
  }

  public void setDateSpa(OffsetDateTime dateSpa) {
    this.dateSpa = dateSpa;
  }

  public String getSpaLegalReference() {
    return spaLegalReference;
  }

  public void setSpaLegalReference(String spaLegalReference) {
    this.spaLegalReference = spaLegalReference;
  }

  public OffsetDateTime getDatePropSci() {
    return datePropSci;
  }

  public void setDatePropSci(OffsetDateTime datePropSci) {
    this.datePropSci = datePropSci;
  }

  public OffsetDateTime getDateConfSci() {
    return dateConfSci;
  }

  public void setDateConfSci(OffsetDateTime dateConfSci) {
    this.dateConfSci = dateConfSci;
  }

  public OffsetDateTime getDateSac() {
    return dateSac;
  }

  public void setDateSac(OffsetDateTime dateSac) {
    this.dateSac = dateSac;
  }

  public String getSacLegalReference() {
    return sacLegalReference;
  }

  public void setSacLegalReference(String sacLegalReference) {
    this.sacLegalReference = sacLegalReference;
  }

  public String getExplanations() {
    return explanations;
  }

  public void setExplanations(String explanations) {
    this.explanations = explanations;
  }

  public Double getLengthKm() {
    return lengthKm;
  }

  public void setLengthKm(Double lengthKm) {
    this.lengthKm = lengthKm;
  }

  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public Double getMarineAreaPercentage() {
    return marineAreaPercentage;
  }

  public void setMarineAreaPercentage(Double marineAreaPercentage) {
    this.marineAreaPercentage = marineAreaPercentage;
  }

  public String getDocumentation() {
    return documentation;
  }

  public void setDocumentation(String documentation) {
    this.documentation = documentation;
  }

  public String getQuality() {
    return quality;
  }

  public void setQuality(String quality) {
    this.quality = quality;
  }

  public String getDesignation() {
    return designation;
  }

  public void setDesignation(String designation) {
    this.designation = designation;
  }

  public String getOthercharact() {
    return othercharact;
  }

  public void setOthercharact(String othercharact) {
    this.othercharact = othercharact;
  }

  public String getInspireId() {
    return inspireId;
  }

  public void setInspireId(String inspireId) {
    this.inspireId = inspireId;
  }
}
