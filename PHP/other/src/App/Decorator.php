<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace App;

/**
 * Class Decorator
 */
class Decorator implements PrinterInterface
{
    /**
     * @var string
     */
    private $textBefore;

    /**
     * @var string
     */
    private $textAfter;

    /**
     * @var PrinterInterface
     */
    private $printer;

    /**
     * Decorator constructor.
     *
     * @param string $textBefore
     * @param string $textAfter
     * @param PrinterInterface $printer
     */
    public function __construct(
        $textBefore,
        $textAfter,
        PrinterInterface $printer
    ) {
        $this->textBefore = $textBefore;
        $this->textAfter = $textAfter;
        $this->printer = $printer;
    }

    /**
     * @return void
     */
    public function printText()
    {
        echo $this->textBefore;
        $this->printer->printText();
        echo $this->textAfter;
    }
}
