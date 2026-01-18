package eu.bmdproject.dataspace.model;

public final class BiseSite extends Site {

  private String siteDescription;
  private String designation;
  private String cddaDesignationNationalLanguage;
  private String majorEcosystemType;
  private Integer yearEstablished; // BISE key: year_stablished (sic)
  private Integer numberProtectedHabitatTypes;
  private Integer numberProtectedSpecies;
  private String regions;
  private String managementPlan;

  public BiseSite() {}

  public String getSiteDescription() {
    return siteDescription;
  }

  public void setSiteDescription(String siteDescription) {
    this.siteDescription = siteDescription;
  }

  public String getDesignation() {
    return designation;
  }

  public void setDesignation(String designation) {
    this.designation = designation;
  }

  public String getCddaDesignationNationalLanguage() {
    return cddaDesignationNationalLanguage;
  }

  public void setCddaDesignationNationalLanguage(String cddaDesignationNationalLanguage) {
    this.cddaDesignationNationalLanguage = cddaDesignationNationalLanguage;
  }

  public String getMajorEcosystemType() {
    return majorEcosystemType;
  }

  public void setMajorEcosystemType(String majorEcosystemType) {
    this.majorEcosystemType = majorEcosystemType;
  }

  public Integer getYearEstablished() {
    return yearEstablished;
  }

  public void setYearEstablished(Integer yearEstablished) {
    this.yearEstablished = yearEstablished;
  }

  public Integer getNumberProtectedHabitatTypes() {
    return numberProtectedHabitatTypes;
  }

  public void setNumberProtectedHabitatTypes(Integer numberProtectedHabitatTypes) {
    this.numberProtectedHabitatTypes = numberProtectedHabitatTypes;
  }

  public Integer getNumberProtectedSpecies() {
    return numberProtectedSpecies;
  }

  public void setNumberProtectedSpecies(Integer numberProtectedSpecies) {
    this.numberProtectedSpecies = numberProtectedSpecies;
  }

  public String getRegions() {
    return regions;
  }

  public void setRegions(String regions) {
    this.regions = regions;
  }

  public String getManagementPlan() {
    return managementPlan;
  }

  public void setManagementPlan(String managementPlan) {
    this.managementPlan = managementPlan;
  }
}
