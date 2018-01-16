package com.isep.archibook.controllers;

import com.isep.archibook.entities.Student;
import com.isep.archibook.services.LdapService;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static com.isep.archibook.ArchibookApplication.getSession;
import static org.unbescape.html.HtmlEscape.escapeHtml4;

@Controller
public class LoginController {

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ModelAndView loginDisplay() {return new ModelAndView("login");}

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ModelAndView loginProcess(ModelAndView modelAndView,
                                     HttpSession httpSession,
                                     @RequestParam("userId") String userId,
                                     @RequestParam("password") String password) {

        String erreur = "Erreur : ";
        String identifiant = secureFieldString(userId);
        String mdp = passWordEncryption(password);

        if (identifiant.isEmpty() || mdp.isEmpty()) {
            erreur = erreur + "veuillez remplir tous les champs du formulaire";
            modelAndView.addObject("erreur", erreur).setViewName("login");
            return modelAndView;
        } else {
            Session sessionHibernate = getSession();
            Student student = (Student) sessionHibernate.createQuery("select s " +
                    "from student s " +
                    "where s.userId like :identifiant")
                    .setParameter("identifiant", identifiant)
                    .uniqueResult();
            sessionHibernate.close();

            if (student == null) {
                erreur = erreur + "identifiant inconnu";
                modelAndView.addObject("erreur", erreur).setViewName("login");
                return modelAndView;
            } else {

                if (!student.getPassword().equals(mdp)) {
                    erreur = erreur + "mot de passe incorrect";
                    modelAndView.addObject("erreur", erreur).setViewName("login");
                    return modelAndView;
                } else {

                    httpSession.setAttribute("id", student.getId());
                    httpSession.setAttribute("school", student.getSchool());
                    modelAndView.setViewName("redirect:/");
                    return modelAndView;

                }
            }
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView home(HttpSession httpSession, ModelAndView modelAndView) {
        if(httpSession.isNew() || httpSession.getAttribute("id") == null) {
            return new ModelAndView("login");
        }
        Session sessionHibernate = getSession();
        Student student = (Student) sessionHibernate.get(Student.class, (int) httpSession.getAttribute("id"));

        if (student == null) {
            return new ModelAndView("/login");
        }

        modelAndView.addObject("studentList",
                sessionHibernate.createQuery("select s from student s where s.school like :school")
                        .setParameter("school", student.getSchool())
                        .list());
        modelAndView.addObject(student);
        modelAndView.setViewName("home");
        sessionHibernate.close();
        return modelAndView;
    }

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public ModelAndView logout(HttpSession httpSession) {
        httpSession.invalidate();
        return new ModelAndView("redirect:/");
    }


    public String secureFieldString (String inputString) {
        return escapeHtml4(inputString.trim().replaceAll("\\\\", ""));
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
