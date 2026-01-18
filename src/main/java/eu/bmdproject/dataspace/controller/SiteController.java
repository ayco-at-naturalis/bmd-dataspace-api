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

import static org.klojang.util.ArrayMethods.implode;

@RestController
@RequestMapping("/sites")
public class SiteController {

  private final SiteDao siteDao;

  public SiteController(SiteDao siteDao) {
    this.siteDao = siteDao;
  }

  @GetMapping("/{id}/metadata")
  public ResponseEntity<Site> getMetadataDefault(@PathVariable("id") String siteCode) {
    // MVP default (demo-friendly). Change to EEA_NATURA2000 when implemented.
    SiteMetadataSource defaultSource = SiteMetadataSource.EUNIS;
    Site site = siteDao.getMetadataBySiteCode(siteCode, defaultSource);
    return ResponseEntity.ok(site);
  }

  @GetMapping("/{id}/metadata/{source}")
  public ResponseEntity<Site> getMetadataBySource(
      @PathVariable("id") String siteCode,
      @PathVariable("source") String siteMetadataSource
  ) {
    SiteMetadataSource src;
    try {
      src = SiteMetadataSource.parse(siteMetadataSource);
    } catch (TypeConversionException e) {
      String fmt = "No such site metadata source: \"%s\". Valid metadata sources: %s";
      String msg = fmt.formatted(siteMetadataSource, implode(SiteMetadataSource.values()));
      throw new BadRequestException(msg);
    }
    if (src == SiteMetadataSource.EEA_NATURA2000) {
      throw new NotImplementedException("EEA_NATURA2000 site metadata not implemented yet");
    }
    Site site = siteDao.getMetadataBySiteCode(siteCode, src);
    return ResponseEntity.ok(site);
  }
}
