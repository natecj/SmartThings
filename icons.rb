

require 'open-uri'
require 'json'


#xml_urls  = ["http://cdn.device-icons.smartthings.com/"]


output_lines = []
output_lines << "<html>"
output_lines << "<head>"
output_lines << "<title>SmartThings Icons</title"
output_lines << "</head>"
output_lines << "<style type='text/css'>"
output_lines << "ul { list-style-type:none; }"
output_lines << "ul li { width:150px; height:150px; padding:5px; margin:10px; float:left; text-align:center; }"
output_lines << "ul li img { border:1px solid black; }"
output_lines << "</style>"
output_lines << "<body>"


json_hash = JSON.parse(open("https://graph.api.smartthings.com/api/devices/icons").read)
json_hash['categories'].each do |category|
  output_lines << "<h2>#{category['name']}</h2>"
  output_lines << "<ul>"
  category['keys'].each do |key|
    next unless key =~ /^st\.#{category['name']}\.(.*)$/
    icon_name = $1
    output_lines << "<li>"
    output_lines << "#{icon_name}"
    output_lines << "<br />"
    output_lines << "<img src=\"http://cdn.device-icons.smartthings.com/#{category['name']}/#{icon_name}.png\" title=\"#{key}\" />"
    output_lines << "</li>"
  end
  output_lines << "</ul>"
  output_lines << "<br clear='both'>"
  output_lines << "<hr />"
end


output_lines << "</body>"
output_lines << "</html>"


# Save output to file
open('icons.html', 'w') do |f|
  f.puts output_lines.join(' ')
end
