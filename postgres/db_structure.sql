CREATE TABLE Users (
    keycloak_ID UUID PRIMARY KEY,
    username VARCHAR(20) NOT NULL,
    profile_summary VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Images (
    image_ID UUID PRIMARY KEY,
    author_ID UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    file_name VARCHAR(41) NOT NULL CHECK (
        file_name ~* '\.(png|jpg|jpeg|gif)$'
    ),
    FOREIGN KEY (author_ID) REFERENCES Users(keycloak_ID) ON DELETE CASCADE
);

CREATE TABLE Comments (
    comment_ID UUID PRIMARY KEY,
    image_ID UUID NOT NULL,
    author_ID UUID NOT NULL,
    comment_text VARCHAR(500) NOT NULL,
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (image_ID) REFERENCES Images(image_ID) ON DELETE CASCADE,
    FOREIGN KEY (author_ID) REFERENCES Users(keycloak_ID) ON DELETE CASCADE
);

CREATE TABLE Ratings (
    rating_ID UUID PRIMARY KEY,
    image_ID UUID NOT NULL,
    author_ID UUID NOT NULL,
    rating INT CHECK (rating BETWEEN 1 AND 5),
    UNIQUE (image_ID, author_ID),
    FOREIGN KEY (image_ID) REFERENCES Images(image_ID) ON DELETE CASCADE,
    FOREIGN KEY (author_ID) REFERENCES Users(keycloak_ID) ON DELETE CASCADE
);

CREATE TABLE Favorites (
    preference_ID UUID PRIMARY KEY,
    user_ID UUID NOT NULL,
    image_ID UUID NOT NULL,
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_ID, image_ID),
    FOREIGN KEY (image_ID) REFERENCES Images(image_ID) ON DELETE CASCADE,
    FOREIGN KEY (user_ID) REFERENCES Users(keycloak_ID) ON DELETE CASCADE
);

CREATE TABLE Tags (
    tag_ID UUID PRIMARY KEY,
    tag_name VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE Image_Tags (
    image_ID UUID NOT NULL,
    tag_ID UUID NOT NULL,
    PRIMARY KEY (image_ID, tag_ID),
    FOREIGN KEY (image_ID) REFERENCES Images(image_ID) ON DELETE CASCADE,
    FOREIGN KEY (tag_ID) REFERENCES Tags(tag_ID) ON DELETE CASCADE
);

CREATE TABLE Data_Logs (
    data_log_ID UUID PRIMARY KEY,
    admin_ID UUID NOT NULL,
    action VARCHAR(10) CHECK (action IN ('IMPORT', 'EXPORT')),
    file_name VARCHAR(255),
    log_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_ID) REFERENCES Users(keycloak_ID)
);