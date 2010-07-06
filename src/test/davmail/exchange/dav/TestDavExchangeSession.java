/*
 * DavMail POP/IMAP/SMTP/CalDav/LDAP Exchange Gateway
 * Copyright (C) 2010  Mickael Guessant
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package davmail.exchange.dav;

import davmail.BundleMessage;
import davmail.Settings;
import davmail.exchange.AbstractExchangeSessionTestCase;
import davmail.exchange.ExchangeSession;
import davmail.ui.tray.DavGatewayTray;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.property.DavProperty;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Webdav specific unit tests
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class TestDavExchangeSession extends AbstractExchangeSessionTestCase {
    DavExchangeSession davSession;

    public void setUp() throws IOException {
        super.setUp();
        davSession = ((DavExchangeSession) session);
    }

    public void testGetFolderPath() {
        String mailPath = davSession.getFolderPath("");
        String rootPath = davSession.getFolderPath("/users/");

        assertEquals(mailPath + davSession.inboxName, davSession.getFolderPath("INBOX"));
        assertEquals(mailPath + davSession.deleteditemsName, davSession.getFolderPath("Trash"));
        assertEquals(mailPath + davSession.sentitemsName, davSession.getFolderPath("Sent"));
        assertEquals(mailPath + davSession.draftsName, davSession.getFolderPath("Drafts"));

        assertEquals(mailPath + davSession.contactsName, davSession.getFolderPath("contacts"));
        assertEquals(mailPath + davSession.calendarName, davSession.getFolderPath("calendar"));

        assertEquals(mailPath + davSession.inboxName + "/test", davSession.getFolderPath("INBOX/test"));
        assertEquals(mailPath + davSession.deleteditemsName + "/test", davSession.getFolderPath("Trash/test"));
        assertEquals(mailPath + davSession.sentitemsName + "/test", davSession.getFolderPath("Sent/test"));
        assertEquals(mailPath + davSession.draftsName + "/test", davSession.getFolderPath("Drafts/test"));

        // TODO: may be wrong, should return full url, public folders may be located on another server
        assertEquals("/public", davSession.getFolderPath("/public"));
        assertEquals("/public/test", davSession.getFolderPath("/public/test"));

        // caldav folder paths
        assertEquals(mailPath, davSession.getFolderPath("/users/" + davSession.getEmail()));
        assertEquals(mailPath, davSession.getFolderPath("/users/" + davSession.getEmail() + '/'));
        assertEquals(mailPath, davSession.getFolderPath("/users/" + davSession.getAlias()));
        assertEquals(mailPath, davSession.getFolderPath("/users/" + davSession.getAlias() + '/'));

        assertEquals(mailPath, davSession.getFolderPath("/users/" + davSession.getEmail().toUpperCase()));
        assertEquals(mailPath, davSession.getFolderPath("/users/" + davSession.getEmail().toLowerCase()));
        assertEquals(mailPath, davSession.getFolderPath("/users/" + davSession.getAlias().toUpperCase()));
        assertEquals(mailPath, davSession.getFolderPath("/users/" + davSession.getAlias().toLowerCase()));


        assertEquals(mailPath + "subfolder", davSession.getFolderPath("/users/" + davSession.getAlias() + "/subfolder"));
        assertEquals(mailPath + "subfolder/", davSession.getFolderPath("/users/" + davSession.getAlias() + "/subfolder/"));

        assertEquals(rootPath + "anotheruser/", davSession.getFolderPath("/users/anotheruser"));
        assertEquals(rootPath + "anotheruser/subfolder", davSession.getFolderPath("/users/anotheruser/subfolder"));

        assertEquals(mailPath + davSession.inboxName, davSession.getFolderPath("/users/" + davSession.getEmail() + "/inbox"));
        assertEquals(mailPath + davSession.inboxName + "/subfolder", davSession.getFolderPath("/users/" + davSession.getEmail() + "/inbox/subfolder"));

        assertEquals(mailPath + davSession.calendarName, davSession.getFolderPath("/users/" + davSession.getEmail() + "/calendar"));
        assertEquals(mailPath + davSession.contactsName, davSession.getFolderPath("/users/" + davSession.getEmail() + "/contacts"));
        assertEquals(mailPath + davSession.contactsName, davSession.getFolderPath("/users/" + davSession.getEmail() + "/addressbook"));

        assertEquals(rootPath + "anotherUser/" + davSession.inboxName, davSession.getFolderPath("/users/anotherUser/inbox"));
        assertEquals(rootPath + "anotherUser/" + davSession.calendarName, davSession.getFolderPath("/users/anotherUser/calendar"));
        assertEquals(rootPath + "anotherUser/" + davSession.contactsName, davSession.getFolderPath("/users/anotherUser/contacts"));

        // do not replace i18n names
        assertEquals(mailPath + "Inbox", davSession.getFolderPath("/users/" + davSession.getEmail() + "/Inbox"));
        assertEquals(mailPath + "Calendar", davSession.getFolderPath("/users/" + davSession.getEmail() + "/Calendar"));
        assertEquals(mailPath + "Contacts", davSession.getFolderPath("/users/" + davSession.getEmail() + "/Contacts"));
    }

    public void testGetCategoryList() throws IOException {
        Set<String> attributes = new HashSet<String>();
        attributes.add("permanenturl");
        attributes.add("roamingxmlstream");
        MultiStatusResponse[] responses = davSession.searchItems("/users/" + davSession.getEmail() + "/calendar", attributes, davSession.and(davSession.isFalse("isfolder"), davSession.equals("messageclass", "IPM.Configuration.CategoryList")), DavExchangeSession.FolderQueryTraversal.Shallow);
        String value = (String) responses[0].getProperties(HttpStatus.SC_OK).get(Field.getPropertyName("roamingxmlstream")).getValue();
        String propertyList = new String(Base64.decodeBase64(value.getBytes()), "UTF-8");

    }

    public void testGetCalendarOptions() throws IOException {
        Set<String> attributes = new HashSet<String>();
        attributes.add("permanenturl");
        attributes.add("roamingxmlstream");
        MultiStatusResponse[] responses = davSession.searchItems("/users/" + davSession.getEmail() + "/calendar", attributes, davSession.and(davSession.isFalse("isfolder"), davSession.equals("messageclass", "IPM.Configuration.Calendar")), DavExchangeSession.FolderQueryTraversal.Shallow);
        String value = (String) responses[0].getProperties(HttpStatus.SC_OK).get(Field.getPropertyName("roamingxmlstream")).getValue();
        String propertyList = new String(Base64.decodeBase64(value.getBytes()), "UTF-8");

    }

    public void testAllHidden() throws IOException {
        Set<String> attributes = new HashSet<String>();
        attributes.add("messageclass");
        attributes.add("permanenturl");
        attributes.add("roamingxmlstream");
        attributes.add("displayname");

        MultiStatusResponse[] responses = davSession.searchItems("/users/" + davSession.getEmail() + "/", attributes, davSession.and(davSession.isTrue("ishidden")), DavExchangeSession.FolderQueryTraversal.Deep);
        for (MultiStatusResponse response : responses) {
            System.out.println(response.getProperties(HttpStatus.SC_OK).get(Field.getPropertyName("messageclass")).getValue() + ": "
                    + response.getProperties(HttpStatus.SC_OK).get(Field.getPropertyName("displayname")).getValue());

            DavProperty roamingxmlstreamProperty = response.getProperties(HttpStatus.SC_OK).get(Field.getPropertyName("roamingxmlstream"));
            if (roamingxmlstreamProperty != null) {
                System.out.println(new String(Base64.decodeBase64(((String) roamingxmlstreamProperty.getValue()).getBytes()), "UTF-8"));
            }

        }
    }

    public void testNonIpmSubtree() throws IOException {
        Set<String> attributes = new HashSet<String>();
        attributes.add("messageclass");
        attributes.add("permanenturl");
        attributes.add("roamingxmlstream");
        attributes.add("roamingdictionary");
        attributes.add("displayname");

        MultiStatusResponse[] responses = davSession.searchItems("/users/" + davSession.getEmail() + "/non_ipm_subtree", attributes, davSession.and(davSession.isTrue("ishidden")), DavExchangeSession.FolderQueryTraversal.Deep);
        for (MultiStatusResponse response : responses) {
            System.out.println(response.getHref() + ' ' + response.getProperties(HttpStatus.SC_OK).get(Field.getPropertyName("messageclass")).getValue() + ": "
                    + response.getProperties(HttpStatus.SC_OK).get(Field.getPropertyName("displayname")).getValue());

            DavProperty roamingxmlstreamProperty = response.getProperties(HttpStatus.SC_OK).get(Field.getPropertyName("roamingxmlstream"));
            if (roamingxmlstreamProperty != null) {
                System.out.println("roamingxmlstream: " + new String(Base64.decodeBase64(((String) roamingxmlstreamProperty.getValue()).getBytes()), "UTF-8"));
            }

            DavProperty roamingdictionaryProperty = response.getProperties(HttpStatus.SC_OK).get(Field.getPropertyName("roamingdictionary"));
            if (roamingdictionaryProperty != null) {
                System.out.println("roamingdictionary: " + new String(Base64.decodeBase64(((String) roamingdictionaryProperty.getValue()).getBytes()), "UTF-8"));
            }


        }
    }

    public void testGetVtimezone() {
        ExchangeSession.VTimezone timezone = davSession.getVTimezone();
        assertNotNull(timezone.timezoneId);
        assertNotNull(timezone.timezoneBody);
        System.out.println(timezone.timezoneId);
        System.out.println(timezone.timezoneBody);
    }

    public void testDumpVtimezones() {
        Properties properties = new Properties() {
            @Override
            public synchronized Enumeration<Object> keys() {
                Enumeration keysEnumeration = super.keys();
                TreeSet<String> sortedKeySet = new TreeSet<String>();
                while (keysEnumeration.hasMoreElements()) {
                    sortedKeySet.add((String) keysEnumeration.nextElement());
                }
                final Iterator<String> sortedKeysIterator = sortedKeySet.iterator();
                return new Enumeration<Object>() {

                    public boolean hasMoreElements() {
                        return sortedKeysIterator.hasNext();
                    }

                    public Object nextElement() {
                        return sortedKeysIterator.next();
                    }
                };
            }

        };
        for (int i = 1; i < 100; i++) {
            Settings.setProperty("davmail.timezoneId", String.valueOf(i));
            ExchangeSession.VTimezone timezone = davSession.getVTimezone();
            if (timezone.timezoneId != null) {
                properties.put(timezone.timezoneId.replaceAll("\\\\", ""), String.valueOf(i));
                System.out.println(timezone.timezoneId + '=' + i);
            }
            davSession.vTimezone = null;
        }
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream("timezoneids.properties");
            properties.store(fileOutputStream, "Timezone ids");
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
        }
    }

    public void testSearchCalendar() throws IOException {
        List<ExchangeSession.Event> events = davSession.searchEvents("/users/" + davSession.getEmail() + "/calendar/test", ExchangeSession.ITEM_PROPERTIES, null);
        for (ExchangeSession.Event event:events) {
            System.out.println(event.getBody());
        }
    }
}