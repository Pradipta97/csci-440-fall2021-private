package edu.montana.csci.csci440.model;

import edu.montana.csci.csci440.util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class Artist extends Model {

    Long artistId;
    String name;
    String originalName;
    static List<Artist> resultList = new LinkedList<>();

    public Artist() {
    }

    private Artist(ResultSet results) throws SQLException {
        name = results.getString("Name");
        artistId = results.getLong("ArtistId");
        originalName = name;
    }

    public List<Album> getAlbums(){
        return Album.getForArtist(artistId);
    }


    public Long getArtistId() {
        return artistId;
    }

    public void setArtist(Artist artist) {
        this.artistId = artist.getArtistId();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) { //optimistic concurrency helper
        originalName = this.name;
        this.name = name;
    }

    public static List<Artist> all() {
        return all(0, Integer.MAX_VALUE);
    }

    public static List<Artist> all(int page, int count) {
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM artists LIMIT ? OFFSET ?"
             )) {stmt.setInt(1, count);
            if (page == 1) {  //paging, starts from 0 for page 1, adds count for 2nd page, and then multiplies count for an unlimited number of subsequent pages
                stmt.setInt(2, 0);
            }
            else if (page == 2) {
                stmt.setInt(2, count);
            }
            else if (page > 2) {
                stmt.setInt(2, (page-1)*count);
            }
            else {
                stmt.setInt(2, 0);
            }
            stmt.setInt(1, count);
            ResultSet results = stmt.executeQuery();
            while (results.next()) {
                resultList.add(new Artist(results));
            }
            return resultList;
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

    public boolean verify() {
        _errors.clear(); // clear any existing errors
        if (name == null || "".equals(name)) {
            addError("Artist Name can't be null or blank!"); //is there an artist?
        }
        return !hasErrors();
    }

    @Override
    public boolean create() {
        if (verify()) {
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO artists (Name) VALUES (?)")) { //simple insert query to enter artist
                stmt.setString(1, this.getName());
                stmt.executeUpdate();
                originalName = name; //set old name for OC
                artistId = DB.getLastID(conn); //get ID for new artist
                return true;
            } catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        } else {
            return false;
        }
    }


    @Override
    public boolean update() {
        if (verify()) {
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                         "UPDATE artists  SET Name = ? WHERE ArtistId=? AND Name = ?")) { //simple update query for artists, checks for optimistic concurrency
                stmt.setString(1, this.getName());
                stmt.setLong(2, this.getArtistId());
                stmt.setString(3, this.originalName);

                if(stmt.executeUpdate() == 0){
                    return false;
                }
                else{
                    stmt.executeUpdate();
                    return true;
                }
            }catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        } else {
            return false;
        }
    }

    public static Artist find(long i) {
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM artists WHERE ArtistId=?")) {
            stmt.setLong(1, i);
            ResultSet results = stmt.executeQuery();
            if (results.next()) {
                return new Artist(results);
            } else {
                return null;
            }
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

}