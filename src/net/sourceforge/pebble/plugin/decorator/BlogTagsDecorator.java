package net.sourceforge.pebble.plugin.decorator;

import net.sourceforge.pebble.domain.BlogEntry;


/**
 * Generates Technorati tag links for inclusion in the body of blog entries,
 * when rendered as HTML and newsfeeds.
 * 
 * @author Simon Brown
 */
public class BlogTagsDecorator extends AbstractTagsDecorator {

  /**
   * Gets the base URL for tag links, complete with trailing slash.
   * 
   * @param blogEntry   the owning BlogEntry
   * @return  a URL as a String
   */ 
  public String getBaseUrl(BlogEntry blogEntry) {
    return blogEntry.getBlog().getUrl() + "tags/";
  }
  
}