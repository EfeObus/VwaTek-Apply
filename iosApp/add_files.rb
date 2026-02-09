require "xcodeproj"

project = Xcodeproj::Project.open("VwaTekApply.xcodeproj")
target = project.targets.first

main_group = project.main_group
iosapp_group = main_group.groups.find { |g| g.name == "iosApp" || g.path == "iosApp" }

unless iosapp_group
  puts "Could not find iosApp group"
  exit 1
end

# Create or find Views group
views_group = iosapp_group.groups.find { |g| g.name == "Views" }
unless views_group
  views_group = iosapp_group.new_group("Views", "Views")
end

# Create or find Utilities group
utilities_group = iosapp_group.groups.find { |g| g.name == "Utilities" }
unless utilities_group
  utilities_group = iosapp_group.new_group("Utilities", "Utilities")
end

# View files to add
view_files = %w[
  StarCoachingSheet.swift
  LinkedInImportSheet.swift
  PDFExportSheet.swift
  VersionHistorySheet.swift
  OptimizerView.swift
]

view_files.each do |filename|
  file_ref = views_group.new_reference(filename)
  target.add_file_references([file_ref])
  puts "Added Views/#{filename}"
end

# Utility files
utility_files = %w[PDFGenerator.swift]
utility_files.each do |filename|
  file_ref = utilities_group.new_reference(filename)
  target.add_file_references([file_ref])
  puts "Added Utilities/#{filename}"
end

project.save
puts "Project saved successfully"
