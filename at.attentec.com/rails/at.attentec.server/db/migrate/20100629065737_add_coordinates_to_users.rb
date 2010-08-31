class AddCoordinatesToUsers < ActiveRecord::Migration
  def self.up
    change_table :users do |t|
      t.float :latitude, :default => nil
      t.float :longitude, :default => nil
    end
  end

  def self.down
    change_table :users do |t|
      t.remove :latitude
      t.remove :longitude
    end
  end
end
