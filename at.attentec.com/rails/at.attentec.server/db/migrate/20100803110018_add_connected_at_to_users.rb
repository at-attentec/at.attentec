class AddConnectedAtToUsers < ActiveRecord::Migration
  def self.up
    add_column :users, :connected_at, :datetime
  end

  def self.down
    remove_column :users, :connected_at
  end
end
