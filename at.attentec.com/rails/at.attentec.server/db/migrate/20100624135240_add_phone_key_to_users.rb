class AddPhoneKeyToUsers < ActiveRecord::Migration
  def self.up
    add_column :users, :phone_key, :string
  end

  def self.down
    remove_column :users, :phone_key
  end
end
