package eu.bmdproject.dataspace.controller;

import eu.bmdproject.dataspace.dao.SiteDao;
import eu.bmdproject.dataspace.exception.BadRequestException;
import eu.bmdproject.dataspace.exception.NotImplementedException;
import eu.bmdproject.dataspace.model.Site;
import eu.bmdproject.dataspace.model.SiteMetadataSource;
import org.klojang.convert.TypeConversionException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.klojang.util.ArrayMethods.implode;

@RestController @RequestMapping("/sites") public class SiteController {

  private final SiteDao dao;

  public SiteController(SiteDao siteDao) {
    this.dao = siteDao;
  }

  @GetMapping("/index")
  public ResponseEntity<Map<String,List<List<String>>>> getSiteIndex() {
    return ResponseEntity.ok(dao.getSiteIndex());
  }

  @GetMapping("/{siteCode}/metadata")
  public ResponseEntity<Site> getMetadataDefault(@PathVariable String siteCode) {
    Site site = dao.getMetadataForSite(siteCode, SiteMetadataSource.DEFAULT);
    return ResponseEntity.ok(site);
  }

  @GetMapping("/{siteCode}/metadata/{source}")
  public ResponseEntity<Site> getMetadataBySource(
      @PathVariable String siteCode,
      @PathVariable("source") String siteMetadataSource) {
    SiteMetadataSource src;
    try {
      src = SiteMetadataSource.parse(siteMetadataSource);
    } catch (TypeConversionException e) {
      String fmt = "No such site metadata source: \"%s\". Valid metadata sources: %s";
      String msg = fmt.formatted(siteMetadataSource, implode(SiteMetadataSource.values()));
      throw new BadRequestException(msg);
    }
    Site site = dao.getMetadataForSite(siteCode, src);
    return ResponseEntity.ok(site);
  }

  @GetMapping("/{siteCode}/geojson")
  public ResponseEntity<?> getGeoJsonForSite(@PathVariable String siteCode) {
    String geoJson = dao.getGeoJsonForSite(siteCode);
    return ResponseEntity.ok(geoJson);
  }

  @GetMapping("/{siteCode}/geopackage")
  public ResponseEntity<?> getGeoPackageForSite(@PathVariable String siteCode) {
    throw new NotImplementedException("GeoPackage download not implemented yet");
  }

}
