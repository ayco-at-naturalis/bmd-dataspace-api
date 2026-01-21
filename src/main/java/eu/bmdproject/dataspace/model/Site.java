package eu.bmdproject.dataspace.model;

public abstract sealed class Site permits DefaultSite, EunisSite, BiseSite {

  // Common denominator (present in both EUNIS + BISE outputs)
  private String siteCode;     // EUNIS: code_site, BISE: site_code
  private String siteName;
  private String countryCode;
  private String countryName;
  private String siteType;
  private Double areaKm2;
  private Double areaHa;

  public Site() {
  }

  public String getSiteCode() {
    return siteCode;
  }

  public void setSiteCode(String siteCode) {
    this.siteCode = siteCode;
  }

  public String getSiteName() {
    return siteName;
  }

  public void setSiteName(String siteName) {
    this.siteName = siteName;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }


  public String getCountryName() {
    return countryName;
  }

  public void setCountryName(String countryName) {
    this.countryName = countryName;
  }

  public String getSiteType() {
    return siteType;
  }

  public void setSiteType(String siteType) {
    this.siteType = siteType;
  }

  public Double getAreaKm2() {
    return areaKm2;
  }

  public void setAreaKm2(Double areaKm2) {
    this.areaKm2 = areaKm2;
  }

  public Double getAreaHa() {
    return areaHa;
  }

  public void setAreaHa(Double areaHa) {
    this.areaHa = areaHa;
  }
}
