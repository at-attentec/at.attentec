#
# Copyright (c) 2010 Attentec AB, http://www.attentec.se
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# 

desc "Remove users older that 1 day (this is good if you are running a demo-server)" 
task :demo_server_remove_old_users => :environment do
  puts ''
  t = Time.new - 1.day
  puts "Removing users created_at < " + t.to_s
  User.find_each(:conditions => ['created_at < ?', t]) do |user|
    puts "\tRemoving user " + user.id.to_s + " " + user.username + ", created at " + user.created_at.to_s
    user.destroy
  end
end