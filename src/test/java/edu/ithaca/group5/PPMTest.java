package edu.ithaca.group5;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PPMTest {
    private PPM ppm;


    @BeforeAll
    public void setup() throws SQLException {
        ppm = new PPM(true);
        ppm.dbConnection.emptyOrderTable();
        ppm.dbConnection.emptyUserTable();
    }

    @Test
    public void login() throws SQLException {
        // No matching user
        assertNull(ppm.login("test", "test"));

        // Client
        Client clientToAdd = new Client(-1, "test", "user", "pass", false);
        ppm.dbConnection.addClient(clientToAdd);
        User user = ppm.login("user", "pass");
        assertEquals(clientToAdd.name, user.name);
        assertEquals(clientToAdd.username, user.username);
        assertTrue(user.isPassword("pass"));
        assertFalse(user.isFrozen);
        assertEquals(clientToAdd.balance, user.balance);
        assertEquals(Client.class, user.getClass());

        // Employee
        Employee empToAdd = new Employee(-1, "test2", "user2", "pass2", false,0, "");
        ppm.dbConnection.addEmployee(empToAdd);
        user = ppm.login("user2", "pass2");
        assertEquals("test2", user.name);
        assertEquals("user2", user.username);
        assertTrue(user.isPassword("pass2"));
        assertFalse(user.isFrozen);
        assertEquals(empToAdd.balance, user.balance);
        assertEquals(Employee.class, user.getClass());

        // Pharmacist
        Pharmacist pharmToAdd = new Pharmacist(-1, "test3", "user3", "pass3");
        ppm.dbConnection.addPharmacist(pharmToAdd);
        user = ppm.login("user3", "pass3");
        assertEquals("test3", user.name);
        assertEquals("user3", user.username);
        assertTrue(user.isPassword("pass3"));
        assertFalse(user.isFrozen);
        assertEquals(pharmToAdd.balance, user.balance);
        assertEquals(Pharmacist.class, user.getClass());


        ppm.dbConnection.addPharmacist(new Pharmacist(-1, "test4", "user4", "pass4"));
        user = ppm.login("user4", "pass4");
        assertNotNull(user);

    }

    @Test
    public void maxLoginAttempts() {
        ppm.dbConnection.addEmployee(new Employee(-1, "test", "username", "pass", false,0, ""));
        for (int i = 0; i < ppm.MAX_LOGIN_ATTEMPTS; i++) {
            User user = ppm.dbConnection.getUserByUsername("username");
            assertFalse(user.isFrozen);
            ppm.login("username", "badpass");
        }
        User user = ppm.dbConnection.getUserByUsername("username");
        assertTrue(user.isFrozen);
    }

    @Test
    public void issueListTest() {
        Issue issue;

        issue = ppm.addIssue("TestIssue", "This is a test");
        assertEquals(1, ppm.issues.size());
        assertNotNull(issue);

        issue = ppm.removeIssue("TestIssue");
        assertEquals(0, ppm.issues.size());
        assertNotNull(issue);
        assertEquals("TestIssue", issue.name);

        ppm.addIssue("Test1", "test");
        issue = ppm.addIssue("Test2", "test");
        ppm.addIssue("Test3", "test");
        assertEquals(3, ppm.issues.size());

        assertTrue(ppm.solveIssue("Test2"));
        assertFalse(ppm.solveIssue("Bad Name"));
        assertEquals(3, ppm.issues.size());
        assertTrue(issue.solved);

        issue = ppm.removeIssue("Bad Name");
        assertNull(issue);

        ppm.clearSolvedIssues();
        assertEquals(2, ppm.issues.size());
        ppm.addIssue("Test2", "test");
        assertEquals(3, ppm.issues.size());
        ppm.clearIssues();
        assertEquals(0, ppm.issues.size());

        issue = ppm.addIssue("Blank", "");
        assertNull(issue);
        assertEquals(0, ppm.issues.size());

    }

    @Test
    public void createUserTest() {
        String currentName = "";
        String currentUsername = "";
        String currentPassword = "";
        Client tempC;
        Employee tempE;
        Pharmacist tempP;
        User tempUser;


        //Creating a user without having an active account
        ppm.activeUser = null;
        try {
            currentName = "testAccount1";
            currentUsername = "testAccountUser";
            currentPassword = "testAccountPass1";

            tempUser = ppm.createUser(currentName, currentUsername, currentPassword, "client");
            assertNull(tempUser);

        } catch (UsernameTakenException e) {
            System.out.println("Error in createUser testing code: redundant username!");
        }

        //Creating a user who already exists
        //Setup active PPM user

        tempP = new Pharmacist(-1, "thePharmacist", "pharmacistmain", "tempP0",0, "");
        ppm.dbConnection.addPharmacist(tempP);
        ppm.activeUser = tempP;
        //ppm.login("pharmacistmain", "tempP0");

        currentName = "testAccount2";
        currentUsername = "testAccountUser2";
        currentPassword = "testAccountPass2";
        try {
            tempUser = ppm.createUser(currentName, currentUsername, currentPassword, "pharmacist");
        } catch (UsernameTakenException e) {
            System.out.println("Error in createUser testing code: redundant username!");
        }

        try {
            tempUser = ppm.createUser(currentName, currentUsername, currentPassword, "pharmacist");
            fail("Failed to throw exception when the desired username was taken");
        }
        catch (UsernameTakenException e){}

        //Different combinations of different types of active users trying to create different types of users
        try {
            //Client as Active User
            tempC = new Client(-1, "theClient", "clientmain", "tempC0", false);
            ppm.activeUser = tempC;
            //CANNOT create a Client
            currentName = "CMadeByC";
            currentUsername = "client1";
            currentPassword = "tempC1";
            tempUser = ppm.createUser(currentName, currentUsername, currentPassword, "client");
            assertNull(tempUser);

            //CANNOT create an Employee
            currentName = "EMadeByC";
            currentUsername = "employee1";
            currentPassword = "tempE1";
            tempUser = ppm.createUser(currentName, currentUsername, currentPassword, "employee");
            assertNull(tempUser);

            //CANNOT create a Pharmacist
            currentName = "PMadeByC";
            currentUsername = "pharmacist1";
            currentPassword = "tempP1";
            tempUser = ppm.createUser(currentName, currentUsername, currentPassword, "pharmacist");
            assertNull(tempUser);



            //Employee as Active User
            tempE = new Employee(-1, "theEmployee", "employeemain", "tempE0", 0);
            ppm.activeUser = tempE;
            //CAN create a Client
            currentName = "CMadeByE";
            currentUsername = "client2";
            currentPassword = "tempC2";
            tempUser = ppm.createUser(currentName, currentUsername, currentPassword, "client");
            assertEquals(tempUser.name, currentName);
            assertEquals(tempUser.username, currentUsername);
            assertTrue(tempUser.isPassword(currentPassword));
            User result = ppm.dbConnection.getUserByUsernameAndPassword(currentUsername, currentPassword);
            if (result != null) {
                assertEquals(result.name, currentName);
                assertEquals(result.username, currentUsername);
                assertTrue(result.isPassword(currentPassword));
                assertEquals(result.getType(), "client");
            }
            else {
                fail("new user was not added to database");
            }

            //CANNOT create an Employee
            currentName = "EMadeByE";
            currentUsername = "employee2";
            currentPassword = "tempE2";
            tempUser = ppm.createUser(currentName, currentUsername, currentPassword, "employee");
            assertNull(tempUser);

            //CANNOT create a Pharmacist
            currentName = "PMadeByE";
            currentUsername = "pharmacist2";
            currentPassword = "tempP2";
            tempUser = ppm.createUser(currentName, currentUsername, currentPassword, "pharmacist");
            assertNull(tempUser);



            //Pharmacist as Active User
            tempP = new Pharmacist(-1, "thePharmacist", "pharmacistmain", "tempP0",0, "");
            ppm.activeUser = tempP;
            //CAN create a Client
            currentName = "CMadeByP";
            currentUsername = "client3";
            currentPassword = "tempC3";
            tempUser = ppm.createUser(currentName, currentUsername, currentPassword, "client");
            assertEquals(tempUser.name, currentName);
            assertEquals(tempUser.username, currentUsername);
            assertTrue(tempUser.isPassword(currentPassword));
            result = ppm.dbConnection.getUserByUsernameAndPassword(currentUsername, currentPassword);
            if (result != null) {
                assertEquals(result.name, currentName);
                assertEquals(result.username, currentUsername);
                assertTrue(result.isPassword(currentPassword));
                assertEquals(result.getType(), "client");
            }
            else {
                fail("new user was not added to database");
            }

            //CAN create an Employee
            currentName = "EMadeByP";
            currentUsername = "employee3";
            currentPassword = "tempE3";
            tempUser = ppm.createUser(currentName, currentUsername, currentPassword, "employee");
            assertEquals(tempUser.name, currentName);
            assertEquals(tempUser.username, currentUsername);
            assertTrue(tempUser.isPassword(currentPassword));
            result = ppm.dbConnection.getUserByUsernameAndPassword(currentUsername, currentPassword);
            if (result != null) {
                assertEquals(result.name, currentName);
                assertEquals(result.username, currentUsername);
                assertTrue(result.isPassword(currentPassword));
                assertEquals(result.getType(), "employee");
            }
            else {
                fail("new user was not added to database");
            }

            //CAN create a Pharmacist
            currentName = "PMadeByP";
            currentUsername = "pharmacist3";
            currentPassword = "tempP3";
            tempUser = ppm.createUser(currentName, currentUsername, currentPassword, "pharmacist");
            assertEquals(tempUser.name, currentName);
            assertEquals(tempUser.username, currentUsername);
            assertTrue(tempUser.isPassword(currentPassword));
            result = ppm.dbConnection.getUserByUsernameAndPassword(currentUsername, currentPassword);
            if (result != null) {
                assertEquals(result.name, currentName);
                assertEquals(result.username, currentUsername);
                assertTrue(result.isPassword(currentPassword));
                assertEquals(result.getType(), "pharmacist");
            }
            else {
                fail("new user was not added to database");
            }

        } catch (UsernameTakenException e) {
            System.out.println("Error in createUser testing code: redundant usernames!");
        }
    }

    @Test
    public void resetPassword() throws NotAuthorizedException {
        ppm.activeUser = new Client(-1, "djskl", "sdjfkld", "sdjfkls", false);
        assertThrows(NotAuthorizedException.class, () -> ppm.resetPassword(ppm.activeUser, ""));
        ppm.activeUser = new Employee(-1, "djskl", "sdjfkld", "sdjfkls", 0);
        assertThrows(NotAuthorizedException.class, () -> ppm.resetPassword(ppm.activeUser, ""));

        ppm.dbConnection.addPharmacist(new Pharmacist(1, "test", "test1", "test2"));
        ppm.dbConnection.addPharmacist(new Pharmacist(1, "test1", "test2", "test3"));



        ppm.login("test1", "test2");
        User toChange = ppm.dbConnection.getUserByUsername("test2");

        ppm.resetPassword(toChange, "NEWPASS");
        toChange = ppm.dbConnection.getUserByUsername("test2");
        assertTrue(toChange.isPassword("NEWPASS"));
    }

    @Test
    public void returnOrder() {
        Client client = new Client(1, "test", "testu", "testp", false);
        ppm.dbConnection.addClient(client);
        ppm.activeUser = client;
        ppm.dbConnection.addOrder("test order", "testu", 5, "n/a","1/1/2001");
        Order o = ppm.dbConnection.getOrderByNameAndUsername("test order", "testu");
        ppm.dbConnection.setPaidTrue(o);
        assertTrue(ppm.returnOrder(o));

        assertEquals(ppm.activeUser.balance, 5);
        o = new Order(324728, "sjdklfds", new Client(22342, "sjdksl", "sdjflds", "sdfjksl", false), 3, "sfjdkl","1/1/2001");
        assertFalse(ppm.returnOrder(o));

    }
}