import UIKit
import PDFKit
import shared

/// Utility class for generating PDF exports of resumes with different templates
class PDFGenerator {
    
    enum Template: String, CaseIterable, Identifiable {
        case professional = "Professional"
        case modern = "Modern"
        case classic = "Classic"
        case minimal = "Minimal"
        
        var id: String { rawValue }
        
        var description: String {
            switch self {
            case .professional:
                return "Clean design with blue accents, ideal for corporate roles"
            case .modern:
                return "Fresh look with teal highlights, great for tech & creative"
            case .classic:
                return "Traditional serif fonts, perfect for academia & law"
            case .minimal:
                return "Simple and elegant with lots of whitespace"
            }
        }
        
        var accentColor: UIColor {
            switch self {
            case .professional:
                return UIColor(red: 26/255, green: 54/255, blue: 93/255, alpha: 1)
            case .modern:
                return UIColor(red: 14/255, green: 165/255, blue: 233/255, alpha: 1)
            case .classic:
                return UIColor(red: 55/255, green: 65/255, blue: 81/255, alpha: 1)
            case .minimal:
                return UIColor(red: 107/255, green: 114/255, blue: 128/255, alpha: 1)
            }
        }
    }
    
    private let pageWidth: CGFloat = 595 // A4 width in points
    private let pageHeight: CGFloat = 842 // A4 height in points
    private let margin: CGFloat = 50
    private let lineHeight: CGFloat = 16
    
    /// Generates a PDF file from a resume with the specified template
    /// - Returns: URL to the generated PDF file
    func generatePDF(from resume: Resume, template: Template) -> URL? {
        let pdfMetaData = [
            kCGPDFContextCreator: "VwaTek Apply",
            kCGPDFContextAuthor: resume.name,
            kCGPDFContextTitle: resume.name
        ]
        
        let format = UIGraphicsPDFRendererFormat()
        format.documentInfo = pdfMetaData as [String: Any]
        
        let pageRect = CGRect(x: 0, y: 0, width: pageWidth, height: pageHeight)
        let renderer = UIGraphicsPDFRenderer(bounds: pageRect, format: format)
        
        let data = renderer.pdfData { context in
            context.beginPage()
            
            switch template {
            case .professional:
                drawProfessionalTemplate(context: context, resume: resume)
            case .modern:
                drawModernTemplate(context: context, resume: resume)
            case .classic:
                drawClassicTemplate(context: context, resume: resume)
            case .minimal:
                drawMinimalTemplate(context: context, resume: resume)
            }
        }
        
        // Save to temporary file
        let fileName = "\(resume.name.replacingOccurrences(of: " ", with: "_"))_\(Int(Date().timeIntervalSince1970)).pdf"
        let tempURL = FileManager.default.temporaryDirectory.appendingPathComponent(fileName)
        
        do {
            try data.write(to: tempURL)
            return tempURL
        } catch {
            print("Failed to save PDF: \(error)")
            return nil
        }
    }
    
    // MARK: - Template Drawing Methods
    
    private func drawProfessionalTemplate(context: UIGraphicsPDFRendererContext, resume: Resume) {
        let titleFont = UIFont.systemFont(ofSize: 28, weight: .bold)
        let sectionFont = UIFont.systemFont(ofSize: 14, weight: .bold)
        let bodyFont = UIFont.systemFont(ofSize: 11)
        
        let blueColor = UIColor(red: 37/255, green: 99/255, blue: 235/255, alpha: 1)
        let darkBlue = UIColor(red: 26/255, green: 54/255, blue: 93/255, alpha: 1)
        
        var y = margin
        
        // Draw name
        let nameAttributes: [NSAttributedString.Key: Any] = [
            .font: titleFont,
            .foregroundColor: darkBlue
        ]
        resume.name.draw(at: CGPoint(x: margin, y: y), withAttributes: nameAttributes)
        y += 40
        
        // Blue line
        let linePath = UIBezierPath()
        linePath.move(to: CGPoint(x: margin, y: y))
        linePath.addLine(to: CGPoint(x: pageWidth - margin, y: y))
        blueColor.setStroke()
        linePath.lineWidth = 2
        linePath.stroke()
        y += 30
        
        // Parse and draw sections
        let sections = parseContent(resume.content)
        for (section, content) in sections {
            // Section header
            let sectionAttributes: [NSAttributedString.Key: Any] = [
                .font: sectionFont,
                .foregroundColor: blueColor
            ]
            section.uppercased().draw(at: CGPoint(x: margin, y: y), withAttributes: sectionAttributes)
            y += 20
            
            // Section content
            let bodyAttributes: [NSAttributedString.Key: Any] = [
                .font: bodyFont,
                .foregroundColor: UIColor.black
            ]
            
            let lines = wrapText(content, font: bodyFont, maxWidth: pageWidth - 2 * margin)
            for line in lines {
                if y > pageHeight - margin { break }
                line.draw(at: CGPoint(x: margin, y: y), withAttributes: bodyAttributes)
                y += lineHeight
            }
            y += 15
        }
    }
    
    private func drawModernTemplate(context: UIGraphicsPDFRendererContext, resume: Resume) {
        let titleFont = UIFont.systemFont(ofSize: 32, weight: .light)
        let sectionFont = UIFont.systemFont(ofSize: 12, weight: .bold)
        let bodyFont = UIFont.systemFont(ofSize: 11)
        
        let accentColor = UIColor(red: 14/255, green: 165/255, blue: 233/255, alpha: 1)
        
        // Accent bar at top
        let rect = CGRect(x: 0, y: 0, width: pageWidth, height: 8)
        accentColor.setFill()
        UIBezierPath(rect: rect).fill()
        
        var y = margin
        
        // Name
        let nameAttributes: [NSAttributedString.Key: Any] = [
            .font: titleFont,
            .foregroundColor: UIColor.black
        ]
        resume.name.draw(at: CGPoint(x: margin, y: y), withAttributes: nameAttributes)
        y += 50
        
        // Sections
        let sections = parseContent(resume.content)
        for (section, content) in sections {
            let sectionAttributes: [NSAttributedString.Key: Any] = [
                .font: sectionFont,
                .foregroundColor: accentColor
            ]
            section.uppercased().draw(at: CGPoint(x: margin, y: y), withAttributes: sectionAttributes)
            y += 18
            
            let bodyAttributes: [NSAttributedString.Key: Any] = [
                .font: bodyFont,
                .foregroundColor: UIColor.darkGray
            ]
            
            let lines = wrapText(content, font: bodyFont, maxWidth: pageWidth - 2 * margin)
            for line in lines {
                if y > pageHeight - margin { break }
                line.draw(at: CGPoint(x: margin, y: y), withAttributes: bodyAttributes)
                y += lineHeight
            }
            y += 20
        }
    }
    
    private func drawClassicTemplate(context: UIGraphicsPDFRendererContext, resume: Resume) {
        let titleFont = UIFont(name: "Times New Roman", size: 24) ?? UIFont.systemFont(ofSize: 24, weight: .bold)
        let sectionFont = UIFont(name: "Times New Roman-Bold", size: 12) ?? UIFont.systemFont(ofSize: 12, weight: .bold)
        let bodyFont = UIFont(name: "Times New Roman", size: 11) ?? UIFont.systemFont(ofSize: 11)
        
        var y = margin + 10
        
        // Centered name
        let nameAttributes: [NSAttributedString.Key: Any] = [
            .font: titleFont,
            .foregroundColor: UIColor.black
        ]
        let nameSize = resume.name.size(withAttributes: nameAttributes)
        resume.name.draw(at: CGPoint(x: (pageWidth - nameSize.width) / 2, y: y), withAttributes: nameAttributes)
        y += 35
        
        // Underline
        let linePath = UIBezierPath()
        linePath.move(to: CGPoint(x: margin, y: y))
        linePath.addLine(to: CGPoint(x: pageWidth - margin, y: y))
        UIColor.black.setStroke()
        linePath.lineWidth = 1
        linePath.stroke()
        y += 25
        
        // Sections
        let sections = parseContent(resume.content)
        for (section, content) in sections {
            let sectionAttributes: [NSAttributedString.Key: Any] = [
                .font: sectionFont,
                .foregroundColor: UIColor.black
            ]
            section.draw(at: CGPoint(x: margin, y: y), withAttributes: sectionAttributes)
            y += 5
            
            let shortLine = UIBezierPath()
            shortLine.move(to: CGPoint(x: margin, y: y))
            shortLine.addLine(to: CGPoint(x: margin + 100, y: y))
            UIColor.black.setStroke()
            shortLine.lineWidth = 0.5
            shortLine.stroke()
            y += 15
            
            let bodyAttributes: [NSAttributedString.Key: Any] = [
                .font: bodyFont,
                .foregroundColor: UIColor.black
            ]
            
            let lines = wrapText(content, font: bodyFont, maxWidth: pageWidth - 2 * margin)
            for line in lines {
                if y > pageHeight - margin { break }
                line.draw(at: CGPoint(x: margin, y: y), withAttributes: bodyAttributes)
                y += lineHeight
            }
            y += 15
        }
    }
    
    private func drawMinimalTemplate(context: UIGraphicsPDFRendererContext, resume: Resume) {
        let titleFont = UIFont.systemFont(ofSize: 22, weight: .thin)
        let sectionFont = UIFont.systemFont(ofSize: 10, weight: .regular)
        let bodyFont = UIFont.systemFont(ofSize: 10)
        
        var y = margin + 20
        
        // Simple name
        let nameAttributes: [NSAttributedString.Key: Any] = [
            .font: titleFont,
            .foregroundColor: UIColor.darkGray
        ]
        resume.name.draw(at: CGPoint(x: margin, y: y), withAttributes: nameAttributes)
        y += 50
        
        // Sections with lots of spacing
        let sections = parseContent(resume.content)
        for (section, content) in sections {
            let sectionAttributes: [NSAttributedString.Key: Any] = [
                .font: sectionFont,
                .foregroundColor: UIColor.gray
            ]
            section.uppercased().draw(at: CGPoint(x: margin, y: y), withAttributes: sectionAttributes)
            y += 20
            
            let bodyAttributes: [NSAttributedString.Key: Any] = [
                .font: bodyFont,
                .foregroundColor: UIColor.darkGray
            ]
            
            let lines = wrapText(content, font: bodyFont, maxWidth: pageWidth - 2 * margin)
            for line in lines {
                if y > pageHeight - margin { break }
                line.draw(at: CGPoint(x: margin, y: y), withAttributes: bodyAttributes)
                y += 14
            }
            y += 25
        }
    }
    
    // MARK: - Helper Methods
    
    private func parseContent(_ content: String) -> [(String, String)] {
        var sections: [(String, String)] = []
        let lines = content.components(separatedBy: "\n")
        
        var currentSection = "Profile"
        var currentContent = ""
        
        let sectionHeaders = ["summary", "objective", "experience", "education",
                             "skills", "certifications", "projects", "awards"]
        
        for line in lines {
            let trimmed = line.trimmingCharacters(in: .whitespaces)
            let lowerLine = trimmed.lowercased()
            
            if sectionHeaders.contains(where: { lowerLine.hasPrefix($0) || lowerLine == $0 }) {
                if !currentContent.isEmpty {
                    sections.append((currentSection, currentContent.trimmingCharacters(in: .whitespaces)))
                }
                currentSection = trimmed.capitalized
                currentContent = ""
            } else if !trimmed.isEmpty {
                currentContent += trimmed + " "
            }
        }
        
        if !currentContent.isEmpty {
            sections.append((currentSection, currentContent.trimmingCharacters(in: .whitespaces)))
        }
        
        if sections.isEmpty && !content.isEmpty {
            sections.append(("Content", content.trimmingCharacters(in: .whitespaces)))
        }
        
        return sections
    }
    
    private func wrapText(_ text: String, font: UIFont, maxWidth: CGFloat) -> [String] {
        let words = text.components(separatedBy: " ")
        var lines: [String] = []
        var currentLine = ""
        
        for word in words {
            let testLine = currentLine.isEmpty ? word : currentLine + " " + word
            let size = testLine.size(withAttributes: [.font: font])
            
            if size.width > maxWidth {
                if !currentLine.isEmpty {
                    lines.append(currentLine)
                    currentLine = word
                } else {
                    lines.append(word)
                }
            } else {
                currentLine = testLine
            }
        }
        
        if !currentLine.isEmpty {
            lines.append(currentLine)
        }
        
        return lines
    }
}
