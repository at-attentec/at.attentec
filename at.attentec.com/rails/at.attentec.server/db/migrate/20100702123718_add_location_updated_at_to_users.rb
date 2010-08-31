class AddLocationUpdatedAtToUsers < ActiveRecord::Migration
  def self.up
    change_table :users do |t|
      t.timestamp :location_updated_at
    end
  end

  def self.down
    change_table :users do |t|
      t.remove :location_updated_at
    end
  end
end
