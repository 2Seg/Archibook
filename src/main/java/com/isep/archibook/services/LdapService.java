package com.isep.archibook.services;

import com.isep.archibook.entities.Student;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static com.isep.archibook.ArchibookApplication.getSession;

@Service
public class LdapService {

    public List<Student> getUsers(String school) throws NamingException{

        Hashtable env = new Hashtable();

        String schoolPort;
        String dc;

        switch (school) {
            case "isep":
                schoolPort = "2000";
                dc = "localdomain";
                break;
            case "audencia":
                schoolPort = "2001";
                dc = "audencia";
                break;
            case "polytechnique":
                schoolPort = "2002";
                dc = "polytechnique";
                break;
            default:
                schoolPort = "2000";
                dc = "localdomain";
                break;
        }

        env.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://127.0.0.1:" + schoolPort);
        env.put(Context.SECURITY_PRINCIPAL, "CN=Directory Manager");
        env.put(Context.SECURITY_CREDENTIALS, "password");

        DirContext context = new InitialDirContext(env);

        String searchFilter = "(objectClass=inetOrgPerson)";
        String[] requiredAttributes = {"uid", "userPassword", "givenName", "sn"};
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningAttributes(requiredAttributes);

        NamingEnumeration<?> users = context.search("dc=" + dc, searchFilter, controls);

        SearchResult searchResult;

        List<Student> studentList = new ArrayList<>();


        while(users.hasMore()){

            Student student = new Student();
            searchResult = (SearchResult) users.next();
            Attributes attr = searchResult.getAttributes();

            student.setUserId((String) attr.get("uid").get());
            student.setPassword(new String((byte[]) attr.get("userPassword").get()));
            student.setFirstName((String) attr.get("givenName").get());
            student.setLastName((String) attr.get("sn").get());
            student.setSchool(school);

            studentList.add(student);
        }

        return studentList;
    }

    public void udpateDb (List<Student> studentList) {

        Session sessionHibernate = getSession();

        for (Student studentLdap : studentList) {
            Student studentCheck = (Student) sessionHibernate.createQuery("select s from student s where s.userId like :userId")
                    .setParameter("userId", studentLdap.getUserId())
                    .uniqueResult();

            sessionHibernate.beginTransaction();

            if (studentCheck == null) {
                studentLdap.setPassword(passWordEncryption(studentLdap.getPassword()));
                sessionHibernate.persist(studentLdap);
            } else {
                if (!studentCheck.getUserId().equals(studentLdap.getUserId())) {
                    studentCheck.setUserId(studentLdap.getUserId());
                }
                if (!studentCheck.getPassword().equals(passWordEncryption(studentLdap.getPassword()))) {
                    studentCheck.setPassword(passWordEncryption(studentLdap.getPassword()));
                }
                if (!studentCheck.getFirstName().equals(studentLdap.getFirstName())) {
                    studentCheck.setFirstName(studentLdap.getFirstName());
                }
                if (!studentCheck.getLastName().equals(studentLdap.getLastName())) {
                    studentCheck.setLastName(studentLdap.getLastName());
                }

                sessionHibernate.update(studentCheck);
            }

            sessionHibernate.getTransaction().commit();
        }

        sessionHibernate.close();

    }

    public String passWordEncryption(String password) {
        if (password.isEmpty()) {
            return password;
        }
        String outputPassword = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes());
            byte[] bytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for(int i=0; i< bytes.length ;i++)
            {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            outputPassword = sb.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        return outputPassword;
    }

}
