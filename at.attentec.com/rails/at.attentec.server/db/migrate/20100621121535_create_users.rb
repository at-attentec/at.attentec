class CreateUsers < ActiveRecord::Migration
  def self.up
    create_table :users do |t|
      t.string :username
      t.string :hashed_password
      t.string :salt
      t.string :first_name
      t.string :last_name
      t.string :address
      t.integer :zipcode
      t.string :city
      t.string :phone
      t.string :email

      t.timestamps
    end
  end

  def self.down
    drop_table :users
  end
end
