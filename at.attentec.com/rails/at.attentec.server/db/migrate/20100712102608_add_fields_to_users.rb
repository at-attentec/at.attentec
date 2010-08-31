class AddFieldsToUsers < ActiveRecord::Migration
  def self.up
    add_column :users, :title, :string
    add_column :users, :degree, :string
    add_column :users, :linkedin_url, :string
    add_column :users, :client, :string
  end

  def self.down
    remove_column :users, :client
    remove_column :users, :linkedin_url
    remove_column :users, :degree
    remove_column :users, :title
  end
end
