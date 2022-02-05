package mflix.api.daos;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteConcern;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import mflix.api.models.Session;
import mflix.api.models.User;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.text.MessageFormat;
import java.util.Map;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Configuration
public class UserDao extends AbstractMFlixDao {

    private final MongoCollection<User> usersCollection;
    //TODO> Ticket: User Management - do the necessary changes so that the sessions collection
    //returns a Session object
    private final MongoCollection<Document> sessionsCollection;

    private final Logger log;

    @Autowired
    public UserDao(
            MongoClient mongoClient, @Value("${spring.mongodb.database}") String databaseName) {
        super(mongoClient, databaseName);
        CodecRegistry pojoCodecRegistry =
                fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(),
                        fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        usersCollection = db.getCollection("users", User.class).withCodecRegistry(pojoCodecRegistry);
        log = LoggerFactory.getLogger(this.getClass());
        //TODO> Ticket: User Management - implement the necessary changes so that the sessions
        // collection returns a Session objects instead of Document objects.
        sessionsCollection = db.getCollection("sessions");
    }

    /**
     * Inserts the `user` object in the `users` collection.
     *
     * @param user - User object to be added
     * @return True if successful, throw IncorrectDaoOperation otherwise
     */
    public boolean addUser(User user) {
        //TODO > Ticket: Durable Writes -  you might want to use a more durable write concern here!

        //TODO > Ticket: Handling Errors - make sure to only add new users
        // and not users that already exist.
        try {
            MongoCollection<User> dup =  usersCollection.withWriteConcern(WriteConcern.MAJORITY);
            dup.insertOne(user);
            //usersCollection.insertOne(user);
            return true;
        }catch (MongoWriteException e){
            log.error(MessageFormat.format("Failed to add user {0}", user.getEmail()));
            return false;
        }


    }

    /**
     * Creates session using userId and jwt token.
     *
     * @param userId - user string identifier
     * @param jwt    - jwt string token
     * @return true if successful
     */
    public boolean createUserSession(String userId, String jwt) {
        //TODO> Ticket: User Management - implement the method that allows session information to be
        // stored in it's designated collection.
        //upsert user_id and jwt into sessions collection
        //if jwt is already in sessions collection, update it
        //if jwt is not in sessions collection, insert it
        try {
            Document session = new Document("user_id", userId).append("jwt", jwt);
            sessionsCollection.updateOne(session, new Document("$set", session), new UpdateOptions().upsert(true));
            return true;
        } catch (MongoWriteException e) {
            return false;
        }

        //TODO > Ticket: Handling Errors - implement a safeguard against
        // creating a session with the same jwt token.
    }

    /**
     * Returns the User object matching the an email string value.
     *
     * @param email - email string to be matched.
     * @return User object or null.
     */
    public User getUser(String email) {
        //User user = null;
        //TODO> Ticket: User Management - implement the query that returns the first User object.
        //return user;
        Bson filter = Filters.eq("email", email);
        FindIterable<User> users = usersCollection.find(filter);
        if (users.first() != null) {
            return users.first();
        }
        return null;
    }

    /**
     * Given the userId, returns a Session object.
     *
     * @param userId - user string identifier.
     * @return Session object or null.
     */
    public Session getUserSession(String userId) {
        //TODO> Ticket: User Management - implement the method that returns Sessions for a given
        // userId
        //get session from sessions collection
        Bson filter = Filters.eq("user_id", userId);
        FindIterable<Document> sessions = sessionsCollection.find(filter);
        if (sessions.first() != null) {
            Session session = new Session();
            session.setUserId(sessions.first().getString("user_id"));
            session.setJwt(sessions.first().getString("jwt"));
            return session;
        }
        return null;
    }

    public boolean deleteUserSessions(String userId) {
        //TODO> Ticket: User Management - implement the delete user sessions method
        //delete sessions from sessions collection
        Bson filter = Filters.eq("user_id", userId);
        DeleteResult result = sessionsCollection.deleteMany(filter);
        return result.getDeletedCount() > 0;
        //return false;
    }

    /**
     * Removes the user document that match the provided email.
     *
     * @param email - of the user to be deleted.
     * @return true if user successfully removed
     */
    public boolean deleteUser(String email) {
        // remove user sessions
        //TODO> Ticket: User Management - implement the delete user method
        //TODO > Ticket: Handling Errors - make this method more robust by
        // handling potential exceptions.
        //deleting user from users collection and handle errors
        try {
            Bson filter = Filters.eq("email", email);
            DeleteResult result = usersCollection.deleteOne(filter);
            return result.getDeletedCount() > 0;
        } catch (MongoWriteException e) {
            return false;
        }
        //return false;
    }

    /**
     * Updates the preferences of an user identified by `email` parameter.
     *
     * @param email           - user to be updated email
     * @param userPreferences - set of preferences that should be stored and replace the existing
     *                        ones. Cannot be set to null value
     * @return User object that just been updated.
     */
    public boolean updateUserPreferences(String email, Map<String, ?> userPreferences) {
        //TODO> Ticket: User Preferences - implement the method that allows for user preferences to
        // be updated.
        //TODO > Ticket: Handling Errors - make this method more robust by
        // handling potential exceptions when updating an entry.
        //add preferences array field into all documents in users collection
        try {
            Bson filter = Filters.eq("email", email);
            Bson update = new Document("$set", new Document("preferences", userPreferences));
            UpdateResult result = usersCollection.updateOne(filter, update, new UpdateOptions().upsert(true));
            return result.getModifiedCount() > 0;
        } catch (MongoWriteException e) {
            return false;
        }
    }
}
