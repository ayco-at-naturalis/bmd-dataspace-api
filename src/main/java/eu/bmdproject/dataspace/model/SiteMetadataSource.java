package eu.bmdproject.dataspace.model;

import org.klojang.convert.EnumParser;

public enum SiteMetadataSource {
  DEFAULT,
  EUNIS,
  BISE;

  private static final EnumParser<SiteMetadataSource> parser = new EnumParser<>(SiteMetadataSource.class);

  public static SiteMetadataSource parse(String raw) {
    return parser.parse(raw);
  }
}
