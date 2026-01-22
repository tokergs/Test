insert into users (id,username,email,password,role) values (default,'admin','admin@admin','$2a$10$jcFLzEfhJKFzwszTFJKEUe29yKLPxOVG6F4tSKLKHDYFmfQAHh04i','USER')

insert into notes (id,title,content,created_at,user_id) values (default,'Basketball','Basketball content',now(),1)
insert into notes (id,title,content,created_at,user_id) values (default,'Brainstorm','Brainstorm content',now(),1)
insert into notes (id,title,content,created_at,user_id) values (default,'Feedback','Feedback content',now(),1)

