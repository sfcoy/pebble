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
package net.sourceforge.pebble.domain;

import net.sourceforge.pebble.comparator.BlogEntryComparator;
import net.sourceforge.pebble.dao.BlogEntryDAO;
import net.sourceforge.pebble.dao.DAOFactory;
import net.sourceforge.pebble.dao.PersistenceException;
import net.sourceforge.pebble.event.blogentry.BlogEntryEvent;

import java.util.*;

/**
 * Represents a blog at a daily level. This manages a collection of BlogEntry instances.
 *
 * @author    Simon Brown
 */
public class DailyBlog extends TimePeriod implements Permalinkable {

  /** the parent, MonthlyBlog instance */
  private MonthlyBlog month;

  /** an integer representing the day that this DailyBlog is for */
  private int day;

  /** the collection of BlogEntry instances */
  private List entries = new ArrayList();

  /**
   * Creates a new DailyBlog for the specified month and day.
   *
   * @param month   a MonthlyBlog instance representing the month
   * @param day     an int representing the day
   */
  DailyBlog(MonthlyBlog month, int day) {
    super(month.getBlog());

    this.month = month;
    this.day = day;
    setDate(getCalendar().getTime());

    if (getBlog() instanceof Blog) {
      try {
        Blog blog = getBlog();

        DAOFactory factory = DAOFactory.getConfiguredFactory();
        BlogEntryDAO dao = factory.getBlogEntryDAO();
        entries = dao.getBlogEntries(this);
        Collections.sort(entries, new BlogEntryComparator());

        Iterator it = entries.iterator();
        while (it.hasNext()) {
          BlogEntry blogEntry = (BlogEntry)it.next();

          if (blogEntry.isApproved()) {
            Iterator categories = blogEntry.getCategories().iterator();
            while (categories.hasNext()) {
              Category category = (Category)categories.next();
              category.addBlogEntry(blogEntry);
            }
            blog.getRootCategory().addBlogEntry(blogEntry);

            // and the blog entry specific tags
            Iterator tags = blogEntry.getTagsAsList().iterator();
            while (tags.hasNext()) {
              Tag tag = (Tag)tags.next();
              tag.addBlogEntry(blogEntry);
            }
          }

          // tell the owning blog that we might have some more recent
          // comments and TrackBacks
          Iterator comments = blogEntry.getComments().iterator();
          while (comments.hasNext()) {
            blog.getResponseManager().addRecentComment((Comment)comments.next());
          }
          Iterator trackBacks = blogEntry.getTrackBacks().iterator();
          while (trackBacks.hasNext()) {
            blog.getResponseManager().addRecentTrackBack((TrackBack)trackBacks.next());
          }

          // now that the entries have been loaded, enable events
          // so that listeners get notified when they change
          blogEntry.setEventsEnabled(true);
        }
      } catch (PersistenceException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Gets a Calendar object representing today.
   *
   * @return    a Calendar instance
   */
  private Calendar getCalendar() {

    // and set the actual date for this daily blog
    Calendar cal = getBlog().getCalendar();
    cal.set(Calendar.YEAR, getMonthlyBlog().getYearlyBlog().getYear());
    cal.set(Calendar.MONTH, getMonthlyBlog().getMonth() - 1);
    cal.set(Calendar.DAY_OF_MONTH, getDay());
    cal.set(Calendar.HOUR_OF_DAY, 12);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    return cal;
  }

  /**
   * Gets a reference to the parent MonthlyBlog instance.
   *
   * @return  a MonthlyBlog instance
   */
  public MonthlyBlog getMonthlyBlog() {
    return month;
  }

  /**
   * Gets the day that this DailyBlog is for.
   *
   * @return    an int representing the day in the month
   */
  public int getDay() {
    return day;
  }

  /**
   * Gets the permalink to display all entries for this DailyBlog.
   *
   * @return  an absolute URL
   */
  public String getPermalink() {
    String s = getBlog().getPermalinkProvider().getPermalink(this);
    if (s != null && s.length() > 0) {
      return getBlog().getUrl() + s.substring(1);
    } else {
      return "";
    }
  }

  /**
   * Gets a Collection containing all the blog entries for this day.
   *
   * @return    an ordered List of BlogEntry instances
   */
  public List getEntries() {
    return Collections.unmodifiableList(entries);
  }

  /**
   * Gets a Collection containing all the blog entries for this day and the
   * specified category.
   *
   * @param   category    a Category instance, or null
   * @return    an ordered List of BlogEntry instances
   */
  public List getEntries(Category category) {
    if (category == null) {
      return this.getEntries();
    } else {
      List blogEntries = new ArrayList();
      Iterator it = getEntries().iterator();
      while (it.hasNext()) {
        BlogEntry blogEntry = (BlogEntry)it.next();
        if (blogEntry.inCategory(category)) {
          blogEntries.add(blogEntry);
        }
      }
      return blogEntries;
    }
  }

  /**
   * Gets a Collection containing all the blog entries for this day and the
   * specified tag.
   *
   * @param     tag    a Strng
   * @return    an ordered List of BlogEntry instances
   */
  public List getEntries(String tag) {
    if (tag == null) {
      return this.getEntries();
    } else {
      List blogEntries = new ArrayList();
      Iterator it = getEntries().iterator();
      while (it.hasNext()) {
        BlogEntry blogEntry = (BlogEntry)it.next();
        if (blogEntry.hasTag(tag)) {
          blogEntries.add(blogEntry);
        }
      }
      return blogEntries;
    }
  }

  /**
   * Gets a specific blog entry.
   *
   * @param entryId   the blog entry id
   * @return  the corresponding BlogEntry instance, or null if a BlogEntry
   *          with the specified id doesn't exist
   */
  public BlogEntry getEntry(String entryId) {
    Iterator it = entries.iterator();
    BlogEntry blogEntry;
    while (it.hasNext()) {
      blogEntry = (BlogEntry)it.next();
      if (blogEntry.getId().equals(entryId)) {
        return blogEntry;
      }
    }
    return null;
  }

  /**
   * Adds a given blog entry.
   *
   * @param entry   the BlogEntry instance to add
   */
  public synchronized void addEntry(BlogEntry entry) {
    if (entry == null) {
      return;
    }

    BlogEntry existing = getEntry(entry.getId());
    if (existing != null && existing != entry) {
      // there is already an entry with the same ID, so increment and try again
      entry.setDate(new Date(entry.getDate().getTime() + 1));
      addEntry(entry);
    } else if (!entries.contains(entry)) {
      entries.add(entry);
      Collections.sort(entries, new BlogEntryComparator());
      entry.setDailyBlog(this);
      entry.setType(BlogEntry.PUBLISHED);

      // now that the entries have been loaded, enable events
      // so that listeners get notified when they change
      entry.setEventsEnabled(true);

      // and notify listeners
      ((Blog)getBlog()).getEventDispatcher().fireBlogEntryEvent(new BlogEntryEvent(entry, BlogEntryEvent.BLOG_ENTRY_ADDED));
    }
  }

  /**
   * Removes a given blog entry.
   *
   * @param entry   the BlogEntry instance to remove
   */
  public synchronized void removeEntry(BlogEntry entry) {
    if (entry != null) {
      ((Blog)getBlog()).getEventDispatcher().fireBlogEntryEvent(new BlogEntryEvent(entry, BlogEntryEvent.BLOG_ENTRY_REMOVED));
      entries.remove(entry);
      entry.setDailyBlog(null);
    }
  }

  /**
   * Determines whether this daily blog has entries.
   *
   * @return    true if this blog contains entries, false otherwise
   */
  public boolean hasEntries() {
    return !entries.isEmpty();
  }

  /**
   * Determines whether this daily blog has entries for a particular category.
   *
   * @param category    the category to test for
   * @return    true if this blog contains entries, false otherwise
   */
  public boolean hasEntries(Category category) {
    if (category == null) {
      return hasEntries();
    } else {
      Iterator it = entries.iterator();
      while (it.hasNext()) {
        BlogEntry entry = (BlogEntry)it.next();
        if (entry.inCategory(category)) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Creates a new BlogEntry instance with the specified values.
   *
   * @param title   the title of the entry
   * @param body    the body of the entry
   * @param date    the date of the entry
   * @return    a new BlogEntry instance
   */
  public BlogEntry createBlogEntry(String title, String body, Date date) {
    BlogEntry blogEntry = new BlogEntry(this);
    blogEntry.setTitle(title);
    blogEntry.setBody(body);
    blogEntry.setDate(date);

    return blogEntry;
  }

  /**
   * Creates a new BlogEntry instance with the specified values.
   *
   * @param date    the date of the entry
   * @return    a new BlogEntry instance
   */
  public BlogEntry createBlogEntry(Date date) {
    BlogEntry blogEntry = new BlogEntry(this);
    blogEntry.setDate(date);
    blogEntry.setType(BlogEntry.NEW);

    return blogEntry;
  }

  /**
   * Creates a new empty BlogEntry instance.
   *
   * @return    a new BlogEntry instance
   */
  public BlogEntry createBlogEntry() {
    Calendar cal = getBlog().getCalendar();
    return createBlogEntry(cal.getTime());
  }

  /**
   * Creates a new empty BlogEntry instance representing a static page.
   *
   * @return    a new BlogEntry instance
   */
  public BlogEntry createStaticPage() {
    BlogEntry blogEntry = createBlogEntry();
    blogEntry.setType(BlogEntry.STATIC_PAGE);
    return blogEntry;
  }

  /**
   * Gets the DailyBlog instance for the previous day.
   *
   * @return    a DailyBlog instance
   */
  public DailyBlog getPreviousDay() {
    return month.getBlogForPreviousDay(this);
  }

  /**
   * Gets the DailyBlog instance for the next day.
   *
   * @return    a DailyBlog instance
   */
  public DailyBlog getNextDay() {
    return month.getBlogForNextDay(this);
  }

  /**
   * Gets the blog entry posted previous (before) to the one specified.
   *
   * @param   blogEntry   a BlogEntry
   * @return  the previous BlogEntry, or null if one doesn't exist
   */
  public BlogEntry getPreviousBlogEntry(BlogEntry blogEntry) {
    int index = entries.indexOf(blogEntry);
    if (index >= 0 && index < (entries.size()-1)) {
      return (BlogEntry)entries.get(index+1);
    } else {
      return null;
    }
  }

  /**
   * Gets the first entry that was posted on this day.
   *
   * @return    a BlogEntry instance, or null is no entries have been posted
   */
  public BlogEntry getFirstBlogEntry() {
    if (!entries.isEmpty()) {
      return (BlogEntry)entries.get(entries.size()-1);
    } else {
      return null;
    }
  }

  /**
   * Gets the last entry that was posted on this day.
   *
   * @return    a BlogEntry instance, or null is no entries have been posted
   */
  public BlogEntry getLastBlogEntry() {
    if (!entries.isEmpty()) {
      return (BlogEntry)entries.get(0);
    } else {
      return null;
    }
  }

  /**
   * Gets the blog entry posted next (afterwards) to the one specified.
   *
   * @param blogEntry   a BlogEntry
   * @return  the next BlogEntry, or null if one doesn't exist
   */
  public BlogEntry getNextBlogEntry(BlogEntry blogEntry) {
    int index = entries.lastIndexOf(blogEntry);
    if (index > 0 && index <= entries.size()) {
      return (BlogEntry)entries.get(index-1);
    } else {
      return null;
    }
  }

  public Date getStartOfDay() {
    Calendar cal = getCalendar();
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 1);

    return cal.getTime();
  }

  public Date getEndOfDay() {
    Calendar cal = getCalendar();
    cal.set(Calendar.HOUR_OF_DAY, 23);
    cal.set(Calendar.MINUTE, 59);
    cal.set(Calendar.SECOND, 59);
    cal.set(Calendar.MILLISECOND, 999);

    return cal.getTime();
  }

}