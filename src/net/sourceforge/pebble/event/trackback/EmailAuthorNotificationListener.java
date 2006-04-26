/*
 * Copyright (c) 2003-2006, Simon Brown
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *
 *   - Neither the name of Pebble nor the names of its contributors may
 *     be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.sourceforge.pebble.event.trackback;

import net.sourceforge.pebble.domain.TrackBack;
import net.sourceforge.pebble.domain.BlogEntry;
import net.sourceforge.pebble.domain.Blog;

import java.util.Collection;
import java.util.HashSet;

/**
 * Sends an e-mail notification to the blog entry author when new
 * TrackBacks are added. This plugin assumes that the author of the
 * blog entry will be used as the e-mail address, with the domain
 * being specified by a plugin property called
 * "EmailAuthorNotificationListener.domain".
 *
 * @author Simon Brown
 */
public class EmailAuthorNotificationListener extends AbstractEmailNotificationListener {

  private static final String DOMAIN_KEY = "EmailAuthorNotificationListener.domain";

  /**
   * Returns the collection of recipients.
   *
   * @param trackBack   the TrackBack from the event
 T  * @return  a Collection of e-mail addresses (Strings)
   */
  protected Collection getEmailAddresses(TrackBack trackBack) {
    BlogEntry blogEntry = trackBack.getBlogEntry();
    Blog blog = blogEntry.getBlog();

    Collection<String> to = new HashSet<String>();
    String domain = blog.getPluginProperties().getProperty(DOMAIN_KEY);
    to.add(blogEntry.getAuthor() + "@" + domain);

    return to;
  }

}