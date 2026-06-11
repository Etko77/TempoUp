UPDATE profiles
SET photo_url = "https://i.pravatar.cc/400?u=" || user_id
WHERE photo_url IS NULL;