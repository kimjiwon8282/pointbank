CREATE DATABASE IF NOT EXISTS auth_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS banking_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS ledger_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS securities_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON auth_db.* TO 'pointbank'@'%';
GRANT ALL PRIVILEGES ON banking_db.* TO 'pointbank'@'%';
GRANT ALL PRIVILEGES ON ledger_db.* TO 'pointbank'@'%';
GRANT ALL PRIVILEGES ON securities_db.* TO 'pointbank'@'%';
FLUSH PRIVILEGES;

-- TODO: split service credentials into auth_user, banking_user, ledger_user, and securities_user.
