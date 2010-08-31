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

# Methods added to this helper will be available to all templates in the application.
module ApplicationHelper
  def qr_code(code, options = {})
    options[:alt] ||= ""
    options[:size] ||= "150"
    html = ''
    if options[:show_code]
      html += '<div class="qr_code_container">'
    end
    html += '<img class="qr_code" src="http://chart.apis.google.com/chart?cht=qr&amp;chs='+options[:size]+'x'+options[:size]+'&amp;chl='+(h code)+'" alt="'+(h options[:alt])+'" />'
    if options[:show_code]
      html += '<br/>'
      html += code
      html += '</div>'
    end
    html
  end
  
  def business_card_link(first_name,last_name)
    url_for :only_path => false, :controller => 'users', :action => 'businesscard', :first_name => first_name, :last_name => last_name
  end

end

def var_dump(variable)
  if defined? content_tag
    content_tag("pre", YAML::dump(variable))
  else
    puts YAML::dump(variable)
  end
end

def p_errors(active_record_object)
  p active_record_object.errors.full_messages
end