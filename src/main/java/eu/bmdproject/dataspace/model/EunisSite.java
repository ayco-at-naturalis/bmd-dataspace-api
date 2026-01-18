package eu.bmdproject.dataspace.model;

public final class EunisSite extends Site {

  // EUNIS-specific fields from your example
  private String eunisAreaCode;
  private String spaDate;
  private String nuts;
  private String adminRegion;
  private Double marinePercent;
  private String sourceDb;
  private String nationalCode;
  private Boolean natura2000;

  public EunisSite() {}

  public String getEunisAreaCode() {
    return eunisAreaCode;
  }

  public void setEunisAreaCode(String eunisAreaCode) {
    this.eunisAreaCode = eunisAreaCode;
  }

  public String getSpaDate() {
    return spaDate;
  }

  public void setSpaDate(String spaDate) {
    this.spaDate = spaDate;
  }

  public String getNuts() {
    return nuts;
  }

  public void setNuts(String nuts) {
    this.nuts = nuts;
  }

  public String getAdminRegion() {
    return adminRegion;
  }

  public void setAdminRegion(String adminRegion) {
    this.adminRegion = adminRegion;
  }

  public Double getMarinePercent() {
    return marinePercent;
  }

  public void setMarinePercent(Double marinePercent) {
    this.marinePercent = marinePercent;
  }

  public String getSourceDb() {
    return sourceDb;
  }

  public void setSourceDb(String sourceDb) {
    this.sourceDb = sourceDb;
  }

  public String getNationalCode() {
    return nationalCode;
  }

  public void setNationalCode(String nationalCode) {
    this.nationalCode = nationalCode;
  }

  public Boolean getNatura2000() {
    return natura2000;
  }

  public void setNatura2000(Boolean natura2000) {
    this.natura2000 = natura2000;
  }
}
