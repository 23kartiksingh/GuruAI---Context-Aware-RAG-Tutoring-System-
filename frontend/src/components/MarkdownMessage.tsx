import ReactMarkdown from 'react-markdown'

/**
 * Renders an assistant reply as markdown.
 *
 * <p>The tutor's system prompt explicitly asks for markdown ("use markdown
 * formatting for code, lists, and formulas"), so rendering the raw string
 * showed literal `**bold**` and `- ` bullets on screen.
 *
 * <p>Tailwind resets heading/list styles globally, so each element gets its
 * classes here rather than relying on default browser styling.
 */
export function MarkdownMessage({ content }: { content: string }) {
  return (
    <div className="text-sm leading-relaxed text-slate-300">
      <ReactMarkdown
        components={{
          p: ({ children }) => <p className="mb-3 last:mb-0">{children}</p>,
          strong: ({ children }) => <strong className="font-semibold text-slate-100">{children}</strong>,
          em: ({ children }) => <em className="italic">{children}</em>,
          ul: ({ children }) => <ul className="mb-3 list-disc space-y-1 pl-5 last:mb-0">{children}</ul>,
          ol: ({ children }) => <ol className="mb-3 list-decimal space-y-1 pl-5 last:mb-0">{children}</ol>,
          li: ({ children }) => <li className="pl-1">{children}</li>,
          h1: ({ children }) => <h1 className="mb-2 mt-4 text-base font-semibold text-white first:mt-0">{children}</h1>,
          h2: ({ children }) => <h2 className="mb-2 mt-4 text-base font-semibold text-white first:mt-0">{children}</h2>,
          h3: ({ children }) => <h3 className="mb-2 mt-3 text-sm font-semibold text-slate-100 first:mt-0">{children}</h3>,
          // Inline code vs fenced blocks: react-markdown passes both through
          // `code`, so the parent <pre> below handles block styling and this
          // only needs to cover the inline case.
          code: ({ children }) => (
            <code className="rounded bg-white/10 px-1.5 py-0.5 font-mono text-[13px] text-cyan-300">
              {children}
            </code>
          ),
          pre: ({ children }) => (
            <pre className="mb-3 overflow-x-auto rounded-lg bg-black/40 p-3 text-[13px] last:mb-0 [&_code]:bg-transparent [&_code]:p-0 [&_code]:text-slate-300">
              {children}
            </pre>
          ),
          a: ({ children, href }) => (
            <a href={href} target="_blank" rel="noreferrer" className="text-purple-400 hover:underline">
              {children}
            </a>
          ),
          blockquote: ({ children }) => (
            <blockquote className="mb-3 border-l-2 border-white/15 pl-3 text-slate-400 last:mb-0">
              {children}
            </blockquote>
          ),
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  )
}
